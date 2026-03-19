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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UploadMediaWorkerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val workerParams = mockk<WorkerParameters>(relaxed = true)
    private val localRepository = mockk<LocalUploadRecordsRepository>()
    private val remoteRepository = mockk<RemoteUploadRecordsRepository>()
    private val contentTypeResolver = mockk<ContentTypeResolver>()
    private val uploadDataSource = mockk<UploadDataSource>()

    private lateinit var worker: UploadMediaWorker

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        worker = UploadMediaWorker(
            context,
            workerParams,
            localRepository,
            remoteRepository,
            contentTypeResolver,
            uploadDataSource
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `returns success when no pending upload records`() = runTest {
        // Arrange
        coEvery { localRepository.getPendingUploadRecords() } returns emptyList()

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `uploads to S3 then saves synced record on successful api call`() = runTest {
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
    fun `skips S3 upload when cloudStoragePath already set`() = runTest {
        // Arrange
        val record = createUploadRecord()
        val createdRecord = record.copy(syncStatus = SyncStatus.SYNCED)
        val slot = slot<List<UploadRecord>>()
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(any()) } returns createdRecord
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs

        // Act
        worker.doWork()

        // Assert
        assertEquals(SyncStatus.SYNCED, slot.captured.first().syncStatus)
        coVerify(exactly = 0) { uploadDataSource.uploadMedia(any()) }
    }

    @Test
    fun `marks record as error when file not found in mediastore`() = runTest {
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
    fun `marks record as error when resolveUri returns null`() = runTest {
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
    fun `marks record as error on S3 permanent failure`() = runTest {
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
    fun `returns retry on S3 network failure`() = runTest {
        // Arrange
        val record = createPendingRecord()
        arrangePendingUploads(record)
        coEvery { uploadDataSource.uploadMedia(any()) } returns
                UploadResult.Error(UploadError.Network("timeout"))

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `marks record as error on permanent api failure`() = runTest {
        // Arrange
        val record = createUploadRecord()
        val slot = slot<List<UploadRecord>>()
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(any()) } throws
                Exception("Unexpected response code 400: Bad Request")
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs

        // Act
        worker.doWork()

        // Assert
        assertEquals(SyncStatus.ERROR, slot.captured.first().syncStatus)
    }

    @Test
    fun `returns retry on temporary api failure`() = runTest {
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
    fun `passes image media type for image content type`() = runTest {
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
    fun `passes video media type for video content type`() = runTest {
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
    fun `processes all records even when one fails permanently`() = runTest {
        // Arrange
        val record1 = createUploadRecord("external_primary_1")
        val record2 = createUploadRecord("external_primary_2")
        coEvery { localRepository.getPendingUploadRecords() } returns listOf(record1, record2)
        coEvery { localRepository.getUploadRecords(any()) } returns listOf(record1)
        every { contentTypeResolver.resolve(any()) } returns "image/jpeg"
        coEvery { remoteRepository.createUploadRecord(any()) } throws
                Exception("Unexpected response code 400: Bad Request")
        coEvery { localRepository.saveUploadRecords(any()) } just runs

        // Act
        worker.doWork()

        // Assert
        coVerify(exactly = 2) { localRepository.saveUploadRecords(any()) }
    }

    @Test
    fun `returns success when all permanent failures are processed`() = runTest {
        // Arrange
        val record = createUploadRecord()
        arrangePendingUploads(record)
        coEvery { remoteRepository.createUploadRecord(any()) } throws
                Exception("Unexpected response code 401: Unauthorized")

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
    }

    private fun arrangePendingUploads(
        record: UploadRecord = createUploadRecord(),
        contentType: String? = "image/jpeg",
        localUri: String? = "content://media/external_primary/images/media/123"
    ) {
        coEvery { localRepository.getPendingUploadRecords() } returns listOf(record)
        coEvery { localRepository.getUploadRecords(any()) } returns listOf(record)
        every { contentTypeResolver.resolve(any()) } returns contentType
        every { contentTypeResolver.resolveUri(any()) } returns localUri
        coEvery { localRepository.saveUploadRecords(any()) } just runs
    }

    private fun createUploadRecord(
        mediaId: String = "external_primary_123"
    ): UploadRecord = UploadRecord(
        mediaId = MediaId.of(mediaId),
        cloudStoragePath = CloudStoragePath.of("private/identity123/uuid.jpg"),
        isDeleted = IsDeleted.of(false),
        syncStatus = SyncStatus.PENDING_UPLOAD,
        mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
    )

    private fun createPendingRecord(
        mediaId: String = "external_primary_123"
    ): UploadRecord = UploadRecord(
        mediaId = MediaId.of(mediaId),
        cloudStoragePath = null,
        isDeleted = IsDeleted.of(false),
        syncStatus = SyncStatus.PENDING_UPLOAD,
        mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
    )
}
