package com.appvoyager.cloudphotos.data.media.worker

import android.content.ContentResolver
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UploadMediaWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val contentResolver = mockk<ContentResolver>(relaxed = true)
    private val workerParams = mockk<WorkerParameters>(relaxed = true)
    private val localRepository = mockk<LocalUploadRecordsRepository>()
    private val remoteRepository = mockk<RemoteUploadRecordsRepository>()

    private lateinit var worker: UploadMediaWorker

    @BeforeEach
    fun setUp() {
        every { context.applicationContext } returns context
        every { context.contentResolver } returns contentResolver
        worker = UploadMediaWorker(context, workerParams, localRepository, remoteRepository)
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
    fun `saves SYNCED record on successful API call`() = runTest {
        // Arrange
        val record = createUploadRecord()
        val createdRecord = record.copy(syncStatus = SyncStatus.SYNCED)
        coEvery { localRepository.getPendingUploadRecords() } returns listOf(record)
        every { contentResolver.getType(any()) } returns "image/jpeg"
        coEvery { remoteRepository.createUploadRecord(any()) } returns createdRecord
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs

        // Act
        worker.doWork()

        // Assert
        assertEquals(SyncStatus.SYNCED, slot.captured.first().syncStatus)
    }

    @Test
    fun `marks record as ERROR when file not found in MediaStore`() = runTest {
        // Arrange
        val record = createUploadRecord()
        coEvery { localRepository.getPendingUploadRecords() } returns listOf(record)
        every { contentResolver.getType(any()) } returns null
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs

        // Act
        worker.doWork()

        // Assert
        assertEquals(SyncStatus.ERROR, slot.captured.first().syncStatus)
    }

    @Test
    fun `marks record as ERROR on permanent API failure`() = runTest {
        // Arrange
        val record = createUploadRecord()
        coEvery { localRepository.getPendingUploadRecords() } returns listOf(record)
        every { contentResolver.getType(any()) } returns "image/jpeg"
        coEvery { remoteRepository.createUploadRecord(any()) } throws
            Exception("Unexpected response code 400: Bad Request")
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs

        // Act
        worker.doWork()

        // Assert
        assertEquals(SyncStatus.ERROR, slot.captured.first().syncStatus)
    }

    @Test
    fun `returns retry on temporary API failure`() = runTest {
        // Arrange
        val record = createUploadRecord()
        coEvery { localRepository.getPendingUploadRecords() } returns listOf(record)
        every { contentResolver.getType(any()) } returns "image/jpeg"
        coEvery { remoteRepository.createUploadRecord(any()) } throws Exception("Network timeout")

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `passes IMAGE mediaType for image content type`() = runTest {
        // Arrange
        val record = createUploadRecord()
        val createdRecord = record.copy(syncStatus = SyncStatus.SYNCED)
        coEvery { localRepository.getPendingUploadRecords() } returns listOf(record)
        every { contentResolver.getType(any()) } returns "image/jpeg"
        val slot = slot<CreateUploadRecordRequest>()
        coEvery { remoteRepository.createUploadRecord(capture(slot)) } returns createdRecord
        coEvery { localRepository.saveUploadRecords(any()) } just runs

        // Act
        worker.doWork()

        // Assert
        assertEquals(MediaType.IMAGE, slot.captured.mediaType)
    }

    @Test
    fun `passes VIDEO mediaType for video content type`() = runTest {
        // Arrange
        val record = createUploadRecord()
        val createdRecord = record.copy(syncStatus = SyncStatus.SYNCED)
        coEvery { localRepository.getPendingUploadRecords() } returns listOf(record)
        every { contentResolver.getType(any()) } returns "video/mp4"
        val slot = slot<CreateUploadRecordRequest>()
        coEvery { remoteRepository.createUploadRecord(capture(slot)) } returns createdRecord
        coEvery { localRepository.saveUploadRecords(any()) } just runs

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
        every { contentResolver.getType(any()) } returns "image/jpeg"
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
        coEvery { localRepository.getPendingUploadRecords() } returns listOf(record)
        every { contentResolver.getType(any()) } returns "image/jpeg"
        coEvery { remoteRepository.createUploadRecord(any()) } throws
            Exception("Unexpected response code 401: Unauthorized")
        coEvery { localRepository.saveUploadRecords(any()) } just runs

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
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
}
