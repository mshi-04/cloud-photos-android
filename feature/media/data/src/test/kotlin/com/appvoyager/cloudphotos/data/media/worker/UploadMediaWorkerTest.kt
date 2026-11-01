package com.appvoyager.cloudphotos.data.media.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.appvoyager.cloudphotos.data.media.datasource.UploadDataSource
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadError
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.model.UploadResult
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.RemoteUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.request.CreateUploadRecordRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import com.appvoyager.cloudphotos.domain.media.valueobject.UploadSuccessCount
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class UploadMediaWorkerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val workerParams = mockk<WorkerParameters>(relaxed = true)
    private val localRepository = mockk<LocalUploadRecordsRepository>()
    private val remoteRepository = mockk<RemoteUploadRecordsRepository>()
    private val contentTypeResolver = mockk<ContentTypeResolver>()
    private val uploadDataSource = mockk<UploadDataSource>()
    private val notificationHelper = mockk<UploadNotificationHelper>(relaxed = true)

    private lateinit var worker: UploadMediaWorker

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        Dispatchers.setMain(testDispatcher)
        worker = UploadMediaWorker(
            context,
            workerParams,
            localRepository,
            remoteRepository,
            contentTypeResolver,
            uploadDataSource,
            notificationHelper
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `doWork returns success when no pending upload records`() = runTest {
        // Arrange
        coEvery { localRepository.getPendingUploadRecords() } returns emptyList()

        // Act
        // Boundary: an empty queue does not display or cancel a progress notification
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 0) { notificationHelper.show(any()) }
        verify(exactly = 0) { notificationHelper.cancel() }
        coVerify(exactly = 0) { remoteRepository.completeUpload(any()) }
    }

    @Test
    fun `doWork sets syncStatus to SYNCED when upload and api call succeed`() = runTest {
        // Arrange
        val record = createPendingRecord()
        val uploadedPath = CloudStoragePath.of("private/identity123/uuid.jpg")
        val createdRecord = record.copy(
            cloudStoragePath = uploadedPath,
            syncStatus = SyncStatus.SYNCED
        )
        val slot = slot<List<UploadRecord>>()
        arrangePendingUploads(record)
        coEvery { uploadDataSource.uploadMedia(any()) } returns UploadResult.Success(uploadedPath)
        coEvery { remoteRepository.createUploadRecord(any()) } returns createdRecord
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs

        // Act
        worker.doWork()

        // Assert
        assertEquals(SyncStatus.SYNCED, slot.captured.first().syncStatus)
    }

    @Test
    fun `doWork ignores uploadMedia when cloudStoragePath is present`() = runTest {
        // Arrange
        val record = createUploadRecord()
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(any()) } returns record.copy(syncStatus = SyncStatus.SYNCED)

        // Act
        worker.doWork()

        // Assert
        coVerify(exactly = 0) { uploadDataSource.uploadMedia(any()) }
    }

    @Test
    fun `doWork ignores uploadMedia when syncStatus is SYNCED`() = runTest {
        // Arrange
        val record = createPendingRecord().copy(syncStatus = SyncStatus.SYNCED)
        arrangePendingUploads(record)

        // Act
        worker.doWork()

        // Assert
        coVerify(exactly = 0) { uploadDataSource.uploadMedia(any()) }
    }

    @Test
    fun `doWork sets syncStatus to ERROR when content type is null`() = runTest {
        // Arrange
        val record = createPendingRecord()
        val slot = slot<List<UploadRecord>>()
        arrangePendingUploads(record, contentType = null)
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs

        // Act
        worker.doWork()

        // Assert
        assertEquals(SyncStatus.ERROR, slot.captured.first().syncStatus)
    }

    @Test
    fun `doWork sets syncStatus to ERROR when resolved uri is null`() = runTest {
        // Arrange
        val record = createPendingRecord()
        val slot = slot<List<UploadRecord>>()
        arrangePendingUploads(record, localUri = null)
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs

        // Act
        worker.doWork()

        // Assert
        assertEquals(SyncStatus.ERROR, slot.captured.first().syncStatus)
    }

    @Test
    fun `doWork sets syncStatus to ERROR when S3 upload returns permanent failure`() = runTest {
        // Arrange
        val record = createPendingRecord()
        val slot = slot<List<UploadRecord>>()
        arrangePendingUploads(record)
        coEvery { uploadDataSource.uploadMedia(any()) } returns
            UploadResult.Error(UploadError.AccessDenied("denied"))
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs

        // Act
        worker.doWork()

        // Assert
        assertEquals(SyncStatus.ERROR, slot.captured.first().syncStatus)
    }

    @Test
    fun `doWork returns retry when S3 upload returns network failure`() = runTest {
        // Arrange
        val record = createPendingRecord()
        arrangePendingUploads(record)
        coEvery { uploadDataSource.uploadMedia(any()) } returns
            UploadResult.Error(UploadError.Network("timeout"))

        // Act
        // Error: a temporary failure still dismisses progress before retrying
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.retry(), result)
        verify(exactly = 1) { notificationHelper.show(1) }
        verify(exactly = 1) { notificationHelper.cancel() }
    }

    @Test
    fun `doWork sets syncStatus to ERROR when api returns permanent failure`() = runTest {
        // Arrange
        val record = createUploadRecord()
        val slot = slot<List<UploadRecord>>()
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(any()) } throws
            Exception("Unexpected response code 400: Bad Request")
        coEvery { uploadDataSource.deleteUploadedObject(any()) } just runs
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs

        // Act
        worker.doWork()

        // Assert
        assertEquals(SyncStatus.ERROR, slot.captured.first().syncStatus)
    }

    @Test
    fun `doWork returns retry when api returns temporary failure`() = runTest {
        // Arrange
        val record = createUploadRecord()
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(any()) } throws Exception("Network timeout")

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `doWork calls deleteUploadedObject when api returns permanent failure`() = runTest {
        // Arrange
        val record = createUploadRecord()
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(any()) } throws
            Exception("Unexpected response code 400: Bad Request")
        coEvery { uploadDataSource.deleteUploadedObject(any()) } just runs

        // Act
        worker.doWork()

        // Assert
        coVerify { uploadDataSource.deleteUploadedObject(any()) }
    }

    @Test
    fun `doWork ignores deleteUploadedObject when api returns temporary failure`() = runTest {
        // Arrange
        val record = createUploadRecord()
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(any()) } throws Exception("Network timeout")

        // Act
        worker.doWork()

        // Assert
        coVerify(exactly = 0) { uploadDataSource.deleteUploadedObject(any()) }
    }

    @Test
    fun `doWork calls createUploadRecord with IMAGE mediaType when content type is image`() = runTest {
        // Arrange
        val record = createUploadRecord()
        val createdRecord = record.copy(syncStatus = SyncStatus.SYNCED)
        val slot = slot<CreateUploadRecordRequest>()
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(capture(slot)) } returns createdRecord

        // Act
        worker.doWork()

        // Assert
        assertEquals(MediaType.IMAGE, slot.captured.mediaType)
    }

    @Test
    fun `doWork calls createUploadRecord with VIDEO mediaType when content type is video`() = runTest {
        // Arrange
        val record = createUploadRecord()
        val createdRecord = record.copy(syncStatus = SyncStatus.SYNCED)
        val slot = slot<CreateUploadRecordRequest>()
        arrangePendingUploads(record, contentType = "video/mp4")
        coEvery { remoteRepository.createUploadRecord(capture(slot)) } returns createdRecord

        // Act
        worker.doWork()

        // Assert
        assertEquals(MediaType.VIDEO, slot.captured.mediaType)
    }

    @Test
    fun `doWork calls saveUploadRecords for each record when one fails permanently`() = runTest {
        // Arrange
        val record1 = createUploadRecord("external_primary_1")
        val record2 = createUploadRecord("external_primary_2")
        coEvery { localRepository.getPendingUploadRecords() } returns listOf(record1, record2)
        coEvery { localRepository.getUploadRecords(any()) } returns listOf(record1)
        every { contentTypeResolver.resolve(any()) } returns "image/jpeg"
        coEvery { remoteRepository.createUploadRecord(any()) } throws
            Exception("Unexpected response code 400: Bad Request")
        coEvery { uploadDataSource.deleteUploadedObject(any()) } just runs
        coEvery { localRepository.saveUploadRecords(any()) } just runs

        // Act
        worker.doWork()

        // Assert
        coVerify(exactly = 2) { localRepository.saveUploadRecords(any()) }
    }

    @Test
    fun `doWork returns success when all failures are permanent`() = runTest {
        // Arrange
        val record = createUploadRecord()
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(any()) } throws
            Exception("Unexpected response code 401: Unauthorized")
        coEvery { uploadDataSource.deleteUploadedObject(any()) } just runs

        // Act
        // Error: permanent failures finish the loop and dismiss progress without retrying
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 1) { notificationHelper.cancel() }
    }

    @Test
    fun `doWork calls completeUpload with success count when all records upload successfully`() = runTest {
        // Arrange
        val first = createUploadRecord("external_primary_1")
        val second = createUploadRecord("external_primary_2")
        val slot = slot<UploadSuccessCount>()
        arrangePendingUploads(first)
        coEvery { localRepository.getPendingUploadRecords() } returns listOf(first, second)
        coEvery { localRepository.getUploadRecords(listOf(second.mediaId)) } returns listOf(second)
        coEvery { remoteRepository.createUploadRecord(match { it.mediaId == first.mediaId }) } returns
            first.copy(syncStatus = SyncStatus.SYNCED)
        coEvery { remoteRepository.createUploadRecord(match { it.mediaId == second.mediaId }) } returns
            second.copy(syncStatus = SyncStatus.SYNCED)
        coEvery { remoteRepository.completeUpload(capture(slot)) } just runs

        // Act
        // Interaction: dismiss progress before sending a single completion for the whole batch
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 1) { notificationHelper.show(2) }
        verify(exactly = 1) { notificationHelper.cancel() }
        coVerify(exactly = 1) { remoteRepository.completeUpload(any()) }
        assertEquals(UploadSuccessCount.of(2), slot.captured)
        coVerifyOrder {
            notificationHelper.show(2)
            remoteRepository.createUploadRecord(any())
            remoteRepository.createUploadRecord(any())
            notificationHelper.cancel()
            remoteRepository.completeUpload(UploadSuccessCount.of(2))
        }
    }

    @Test
    fun `doWork ignores completeUpload when successCount is zero`() = runTest {
        // Arrange
        val record = createUploadRecord()
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(any()) } throws
            Exception("Unexpected response code 401: Unauthorized")
        coEvery { uploadDataSource.deleteUploadedObject(any()) } just runs

        // Act
        // Boundary: dismiss progress even when no record succeeds, without announcing completion
        worker.doWork()

        // Assert
        coVerify(exactly = 0) { remoteRepository.completeUpload(any()) }
        verify(exactly = 1) { notificationHelper.cancel() }
    }

    @Test
    fun `doWork ignores completeUpload when hasTemporaryFailure is true`() = runTest {
        // Arrange
        val first = createUploadRecord("external_primary_1")
        val second = createPendingRecord("external_primary_2")
        arrangePendingUploads(first)
        coEvery { localRepository.getPendingUploadRecords() } returns listOf(first, second)
        coEvery { localRepository.getUploadRecords(listOf(second.mediaId)) } returns listOf(second)
        coEvery { remoteRepository.createUploadRecord(any()) } returns first.copy(syncStatus = SyncStatus.SYNCED)
        coEvery { uploadDataSource.uploadMedia(any()) } returns
            UploadResult.Error(UploadError.Network("timeout"))

        // Act
        // Boundary: a temporary failure suppresses completion even after an earlier success
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.retry(), result)
        coVerify(exactly = 1) { remoteRepository.createUploadRecord(any()) }
        coVerify(exactly = 0) { remoteRepository.completeUpload(any()) }
        verify(exactly = 1) { notificationHelper.cancel() }
    }

    @Test
    fun `doWork returns success when completeUpload throws`() = runTest {
        // Arrange
        val record = createUploadRecord()
        val createdRecord = record.copy(syncStatus = SyncStatus.SYNCED)
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(any()) } returns createdRecord
        coEvery { remoteRepository.completeUpload(any()) } throws Exception("Network error")

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork rethrows database failure when a later record cannot be reloaded`() = runTest {
        // Arrange
        val first = createUploadRecord("external_primary_1")
        val second = createUploadRecord("external_primary_2")
        val failure = IllegalStateException("Database unavailable")
        arrangePendingUploads(first)
        coEvery { localRepository.getPendingUploadRecords() } returns listOf(first, second)
        coEvery { remoteRepository.createUploadRecord(any()) } returns first.copy(syncStatus = SyncStatus.SYNCED)
        coEvery { localRepository.getUploadRecords(listOf(second.mediaId)) } throws failure

        // Act
        // Error: dismiss progress on an uncaught DB failure without completing a partially processed batch
        val thrown = assertThrows<IllegalStateException> { worker.doWork() }

        // Assert
        assertSame(failure, thrown)
        verify(exactly = 1) { notificationHelper.show(2) }
        verify(exactly = 1) { notificationHelper.cancel() }
        coVerify(exactly = 1) { remoteRepository.createUploadRecord(any()) }
        coVerify(exactly = 1) { localRepository.saveUploadRecords(listOf(first.copy(syncStatus = SyncStatus.SYNCED))) }
        coVerify(exactly = 0) { remoteRepository.completeUpload(any()) }
        coVerifyOrder {
            remoteRepository.createUploadRecord(any())
            localRepository.getUploadRecords(listOf(second.mediaId))
            notificationHelper.cancel()
        }
    }

    @Test
    fun `doWork rethrows cancellation when a running upload is cancelled`() = runTest {
        // Arrange
        val record = createPendingRecord()
        arrangePendingUploads(record)
        coEvery { uploadDataSource.uploadMedia(any()) } coAnswers { awaitCancellation() }
        val work = async { worker.doWork() }
        runCurrent()
        coVerify(exactly = 1) { uploadDataSource.uploadMedia(any()) }
        verify(exactly = 1) { notificationHelper.show(1) }
        verify(exactly = 0) { notificationHelper.cancel() }

        // Act
        // Coroutine: cancelling the suspended worker dismisses progress without returning success or retry
        work.cancelAndJoin()

        // Assert
        assertThrows<CancellationException> { work.await() }
        verify(exactly = 1) { notificationHelper.cancel() }
        coVerify(exactly = 0) { localRepository.saveUploadRecords(any()) }
        coVerify(exactly = 0) { remoteRepository.createUploadRecord(any()) }
        coVerify(exactly = 0) { remoteRepository.completeUpload(any()) }
    }

    @Test
    fun `doWork rethrows cancellation when S3 cleanup is cancelled`() = runTest {
        // Arrange
        val record = createUploadRecord()
        val cancellation = CancellationException("Cleanup cancelled")
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(any()) } throws
            Exception("Unexpected response code 400: Bad Request")
        coEvery { uploadDataSource.deleteUploadedObject(any()) } throws cancellation

        // Act
        // Coroutine: cancellation inside nested cleanup also dismisses progress and skips completion
        val thrown = assertThrows<CancellationException> { worker.doWork() }

        // Assert
        assertEquals(cancellation.message, thrown.message)
        verify(exactly = 1) { notificationHelper.show(1) }
        verify(exactly = 1) { notificationHelper.cancel() }
        coVerify(exactly = 1) { uploadDataSource.deleteUploadedObject(requireNotNull(record.cloudStoragePath)) }
        coVerify(exactly = 0) { localRepository.saveUploadRecords(any()) }
        coVerify(exactly = 0) { remoteRepository.completeUpload(any()) }
    }

    @Test
    fun `doWork rethrows cancellation when completion request is cancelled`() = runTest {
        // Arrange
        val record = createUploadRecord()
        val cancellation = CancellationException("Completion cancelled")
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(any()) } returns record.copy(syncStatus = SyncStatus.SYNCED)
        coEvery { remoteRepository.completeUpload(any()) } throws cancellation

        // Act
        // Coroutine: progress has already ended when cancellation interrupts the completion request
        val thrown = assertThrows<CancellationException> { worker.doWork() }

        // Assert
        assertEquals(cancellation.message, thrown.message)
        verify(exactly = 1) { notificationHelper.cancel() }
        coVerify(exactly = 1) { remoteRepository.completeUpload(UploadSuccessCount.of(1)) }
        coVerifyOrder {
            remoteRepository.createUploadRecord(any())
            notificationHelper.cancel()
            remoteRepository.completeUpload(UploadSuccessCount.of(1))
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `doWork rethrows database failure when notification cleanup fails`(cancelWithCancellation: Boolean) = runTest {
        // Arrange
        val record = createUploadRecord()
        val databaseFailure = IllegalStateException("Database unavailable")
        val cancelFailure = if (cancelWithCancellation) {
            CancellationException("Notification cancellation interrupted")
        } else {
            IllegalStateException("Notification service unavailable")
        }
        arrangePendingUploads(record)
        coEvery { localRepository.getUploadRecords(any()) } throws databaseFailure
        every { notificationHelper.cancel() } throws cancelFailure

        // Act
        // Error: cleanup failures are suppressed on the original database exception, including cancellation
        val thrown = assertThrows<IllegalStateException> { worker.doWork() }

        // Assert
        assertSame(databaseFailure, thrown)
        assertEquals(listOf(cancelFailure), thrown.suppressed.toList())
        verify(exactly = 1) { notificationHelper.cancel() }
        coVerify(exactly = 0) { remoteRepository.completeUpload(any()) }
    }

    @Test
    fun `doWork rethrows upload cancellation when notification cancel also fails`() = runTest {
        // Arrange
        val record = createPendingRecord()
        val cancellation = CancellationException("Upload cancelled")
        val cancelFailure = IllegalStateException("Notification service unavailable")
        arrangePendingUploads(record)
        coEvery { uploadDataSource.uploadMedia(any()) } throws cancellation
        every { notificationHelper.cancel() } throws cancelFailure

        // Act
        // Coroutine: a notification failure must not turn an upload cancellation into a different failure
        val thrown = assertThrows<CancellationException> { worker.doWork() }

        // Assert
        assertSame(cancellation, thrown)
        assertEquals(listOf(cancelFailure), thrown.suppressed.toList())
        verify(exactly = 1) { notificationHelper.cancel() }
        coVerify(exactly = 0) { localRepository.saveUploadRecords(any()) }
        coVerify(exactly = 0) { remoteRepository.completeUpload(any()) }
    }

    @Test
    fun `doWork rethrows notification show failure when showing progress fails`() = runTest {
        // Arrange
        val record = createUploadRecord()
        val showFailure = SecurityException("Cannot display notification")
        arrangePendingUploads(record)
        every { notificationHelper.show(1) } throws showFailure

        // Act
        // Error: attempt cleanup once even if displaying progress fails before record processing begins
        val thrown = assertThrows<SecurityException> { worker.doWork() }

        // Assert
        assertSame(showFailure, thrown)
        verify(exactly = 1) { notificationHelper.show(1) }
        verify(exactly = 1) { notificationHelper.cancel() }
        coVerify(exactly = 0) { localRepository.getUploadRecords(any()) }
        coVerify(exactly = 0) { uploadDataSource.uploadMedia(any()) }
        coVerify(exactly = 0) { remoteRepository.completeUpload(any()) }
        coVerifyOrder {
            notificationHelper.show(1)
            notificationHelper.cancel()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `doWork rethrows cancel failure when processing succeeds`(cancelWithCancellation: Boolean) = runTest {
        // Arrange
        val record = createUploadRecord()
        val cancelFailure = if (cancelWithCancellation) {
            CancellationException("Notification cancellation interrupted")
        } else {
            IllegalStateException("Notification service unavailable")
        }
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(any()) } returns record.copy(syncStatus = SyncStatus.SYNCED)
        every { notificationHelper.cancel() } throws cancelFailure

        // Act
        // Error/Coroutine: without an earlier failure, notification errors and cancellation still propagate
        val thrown = assertThrows<Exception> { worker.doWork() }

        // Assert
        assertSame(cancelFailure, thrown)
        verify(exactly = 1) { notificationHelper.cancel() }
        coVerify(exactly = 1) { remoteRepository.createUploadRecord(any()) }
        coVerify(exactly = 0) { remoteRepository.completeUpload(any()) }
    }

    private fun arrangePendingUploads(
        record: UploadRecord = createUploadRecord(),
        contentType: String? = "image/jpeg",
        localUri: MediaUrl? = MediaUrl.of("content://media/external_primary/images/media/123")
    ) {
        coEvery { localRepository.getPendingUploadRecords() } returns listOf(record)
        coEvery { localRepository.getUploadRecords(any()) } returns listOf(record)
        every { contentTypeResolver.resolve(any()) } returns contentType
        every { contentTypeResolver.resolveUri(any()) } returns localUri
        coEvery { localRepository.saveUploadRecords(any()) } just runs
        coEvery { remoteRepository.completeUpload(any()) } just runs
    }

    private fun createUploadRecord(mediaId: String = "external_primary_123"): UploadRecord = UploadRecord(
        mediaId = MediaId.of(mediaId),
        cloudStoragePath = CloudStoragePath.of("private/identity123/uuid.jpg"),
        isDeleted = IsDeleted.of(false),
        syncStatus = SyncStatus.PENDING_UPLOAD,
        mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
    )

    private fun createPendingRecord(mediaId: String = "external_primary_123"): UploadRecord = UploadRecord(
        mediaId = MediaId.of(mediaId),
        cloudStoragePath = null,
        isDeleted = IsDeleted.of(false),
        syncStatus = SyncStatus.PENDING_UPLOAD,
        mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
    )
}
