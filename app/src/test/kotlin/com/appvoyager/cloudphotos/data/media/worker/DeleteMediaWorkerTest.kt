package com.appvoyager.cloudphotos.data.media.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.RemoteUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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
import org.junit.jupiter.api.assertThrows
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteMediaWorkerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val workerParams = mockk<WorkerParameters>(relaxed = true)
    private val localRepository = mockk<LocalUploadRecordsRepository>()
    private val remoteRepository = mockk<RemoteUploadRecordsRepository>()

    private lateinit var worker: DeleteMediaWorker

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        worker = DeleteMediaWorker(context, workerParams, localRepository, remoteRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `returns success when no pending delete records`() = runTest {
        // Arrange
        coEvery { localRepository.getPendingDeleteRecords() } returns emptyList()

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `deletes from s3 api and room on success`() = runTest {
        // Arrange
        val record = createUploadRecord("media-1")
        val cloudStoragePath = record.cloudStoragePath!!
        coEvery { localRepository.getPendingDeleteRecords() } returns listOf(record)
        coEvery { remoteRepository.deleteStorageFile(any()) } just runs
        coEvery { remoteRepository.deleteUploadRecord(any()) } just runs
        coEvery { localRepository.deleteUploadRecord(any()) } just runs

        // Act
        worker.doWork()

        // Assert
        coVerifyOrder {
            remoteRepository.deleteStorageFile(cloudStoragePath)
            remoteRepository.deleteUploadRecord(record.mediaId)
            localRepository.deleteUploadRecord(record.mediaId)
        }
    }

    @Test
    fun `marks record as error on permanent api failure`() = runTest {
        // Arrange
        val record = createUploadRecord("media-1")
        coEvery { localRepository.getPendingDeleteRecords() } returns listOf(record)
        coEvery { remoteRepository.deleteStorageFile(any()) } just runs
        coEvery { remoteRepository.deleteUploadRecord(any()) } throws
                Exception("Unexpected response code 403: Forbidden")
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs

        // Act
        worker.doWork()

        // Assert
        assertEquals(SyncStatus.ERROR, slot.captured.first().syncStatus)
    }

    @Test
    fun `returns retry on temporary s3 failure`() = runTest {
        // Arrange
        val record = createUploadRecord("media-1")
        coEvery { localRepository.getPendingDeleteRecords() } returns listOf(record)
        coEvery { remoteRepository.deleteStorageFile(any()) } throws Exception("Network timeout")

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `does not delete from room when s3 fails`() = runTest {
        // Arrange
        val record = createUploadRecord("media-1")
        coEvery { localRepository.getPendingDeleteRecords() } returns listOf(record)
        coEvery { remoteRepository.deleteStorageFile(any()) } throws Exception("Network timeout")

        // Act
        worker.doWork()

        // Assert
        coVerify(exactly = 0) { localRepository.deleteUploadRecord(any()) }
    }

    @Test
    fun `processes remaining records when one fails temporarily`() = runTest {
        // Arrange
        val record1 = createUploadRecord("media-1")
        val record2 = createUploadRecord("media-2")
        val path1 = record1.cloudStoragePath!!
        val path2 = record2.cloudStoragePath!!
        coEvery { localRepository.getPendingDeleteRecords() } returns listOf(record1, record2)
        coEvery { remoteRepository.deleteStorageFile(path1) } throws Exception("Network timeout")
        coEvery { remoteRepository.deleteStorageFile(path2) } just runs
        coEvery { remoteRepository.deleteUploadRecord(any()) } just runs
        coEvery { localRepository.deleteUploadRecord(any()) } just runs

        // Act
        worker.doWork()

        // Assert
        coVerify(exactly = 1) { localRepository.deleteUploadRecord(record2.mediaId) }
    }

    @Test
    fun `returns retry when at least one record has temporary failure`() = runTest {
        // Arrange
        val record1 = createUploadRecord("media-1")
        val record2 = createUploadRecord("media-2")
        val path1 = record1.cloudStoragePath!!
        val path2 = record2.cloudStoragePath!!
        coEvery { localRepository.getPendingDeleteRecords() } returns listOf(record1, record2)
        coEvery { remoteRepository.deleteStorageFile(path1) } throws Exception("Network timeout")
        coEvery { remoteRepository.deleteStorageFile(path2) } just runs
        coEvery { remoteRepository.deleteUploadRecord(any()) } just runs
        coEvery { localRepository.deleteUploadRecord(any()) } just runs

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `returns success when all records deleted successfully`() = runTest {
        // Arrange
        val record1 = createUploadRecord("media-1")
        val record2 = createUploadRecord("media-2")
        coEvery { localRepository.getPendingDeleteRecords() } returns listOf(record1, record2)
        coEvery { remoteRepository.deleteUploadRecord(any()) } just runs
        coEvery { remoteRepository.deleteStorageFile(any()) } just runs
        coEvery { localRepository.deleteUploadRecord(any()) } just runs

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `deletes local record when cloudStoragePath is null`() = runTest {
        // Arrange
        val record = UploadRecord(
            mediaId = MediaId.of("media-1"),
            cloudStoragePath = null,
            isDeleted = IsDeleted.of(true),
            syncStatus = SyncStatus.PENDING_DELETE,
            mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
        )
        coEvery { localRepository.getPendingDeleteRecords() } returns listOf(record)
        coEvery { localRepository.deleteUploadRecord(any()) } just runs

        // Act
        worker.doWork()

        // Assert
        coVerify { localRepository.deleteUploadRecord(record.mediaId) }
    }

    @Test
    fun `does not delete storage file when cloudStoragePath is null`() = runTest {
        // Arrange
        val record = UploadRecord(
            mediaId = MediaId.of("media-1"),
            cloudStoragePath = null,
            isDeleted = IsDeleted.of(true),
            syncStatus = SyncStatus.PENDING_DELETE,
            mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
        )
        coEvery { localRepository.getPendingDeleteRecords() } returns listOf(record)
        coEvery { localRepository.deleteUploadRecord(any()) } just runs

        // Act
        worker.doWork()

        // Assert
        coVerify(exactly = 0) { remoteRepository.deleteStorageFile(any()) }
    }

    @Test
    fun `does not delete remote record when cloudStoragePath is null`() = runTest {
        // Arrange
        val record = UploadRecord(
            mediaId = MediaId.of("media-1"),
            cloudStoragePath = null,
            isDeleted = IsDeleted.of(true),
            syncStatus = SyncStatus.PENDING_DELETE,
            mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
        )
        coEvery { localRepository.getPendingDeleteRecords() } returns listOf(record)
        coEvery { localRepository.deleteUploadRecord(any()) } just runs

        // Act
        worker.doWork()

        // Assert
        coVerify(exactly = 0) { remoteRepository.deleteUploadRecord(any()) }
    }

    @Test
    fun `returns success when cloudStoragePath is null`() = runTest {
        // Arrange
        val record = UploadRecord(
            mediaId = MediaId.of("media-1"),
            cloudStoragePath = null,
            isDeleted = IsDeleted.of(true),
            syncStatus = SyncStatus.PENDING_DELETE,
            mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
        )
        coEvery { localRepository.getPendingDeleteRecords() } returns listOf(record)
        coEvery { localRepository.deleteUploadRecord(any()) } just runs

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `rethrows CancellationException`() = runTest {
        // Arrange
        val record = createUploadRecord("media-1")
        coEvery { localRepository.getPendingDeleteRecords() } returns listOf(record)
        coEvery { remoteRepository.deleteStorageFile(any()) } throws CancellationException()

        // Act & Assert
        assertThrows<CancellationException> {
            worker.doWork()
        }
    }

    private fun createUploadRecord(mediaId: String): UploadRecord = UploadRecord(
        mediaId = MediaId.of(mediaId),
        cloudStoragePath = CloudStoragePath.of("private/identity123/$mediaId.jpg"),
        isDeleted = IsDeleted.of(true),
        syncStatus = SyncStatus.PENDING_DELETE,
        mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
    )
}
