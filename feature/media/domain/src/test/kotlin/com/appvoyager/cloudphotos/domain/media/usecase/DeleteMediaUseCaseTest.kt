package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.repository.DeleteScheduler
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
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
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeleteMediaUseCaseTest {

    private val localRepository = mockk<LocalUploadRecordsRepository>()
    private val deleteScheduler = mockk<DeleteScheduler>()

    private lateinit var useCase: DeleteMediaUseCase

    @BeforeEach
    fun setUp() {
        useCase = DeleteMediaUseCase(localRepository, deleteScheduler)
    }

    @Test
    fun `saved record has isDeleted set to true for synced record`() = runTest {
        // Arrange
        val record = createUploadRecord(syncStatus = SyncStatus.SYNCED)
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs
        every { deleteScheduler.scheduleDelete() } just runs

        // Act
        useCase(record)

        // Assert
        assertEquals(IsDeleted.of(true), slot.captured.first().isDeleted)
    }

    @Test
    fun `saved record has syncStatus set to PENDING_DELETE for synced record`() = runTest {
        // Arrange
        val record = createUploadRecord(syncStatus = SyncStatus.SYNCED)
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs
        every { deleteScheduler.scheduleDelete() } just runs

        // Act
        useCase(record)

        // Assert
        assertEquals(SyncStatus.PENDING_DELETE, slot.captured.first().syncStatus)
    }

    @Test
    fun `saveUploadRecords is called once for synced record`() = runTest {
        // Arrange
        val record = createUploadRecord(syncStatus = SyncStatus.SYNCED)
        coEvery { localRepository.saveUploadRecords(any()) } just runs
        every { deleteScheduler.scheduleDelete() } just runs

        // Act
        useCase(record)

        // Assert
        coVerify(exactly = 1) { localRepository.saveUploadRecords(any()) }
    }

    @Test
    fun `scheduleDelete is called for synced record`() = runTest {
        // Arrange
        val record = createUploadRecord(syncStatus = SyncStatus.SYNCED)
        coEvery { localRepository.saveUploadRecords(any()) } just runs
        every { deleteScheduler.scheduleDelete() } just runs

        // Act
        useCase(record)

        // Assert
        verify { deleteScheduler.scheduleDelete() }
    }

    @Test
    fun `pending upload record is physically deleted from Room`() = runTest {
        // Arrange
        val mediaId = MediaId.of("media-1")
        val record = createUploadRecord(mediaId = mediaId, syncStatus = SyncStatus.PENDING_UPLOAD)
        coEvery { localRepository.deleteUploadRecord(any()) } just runs

        // Act
        useCase(record)

        // Assert
        coVerify(exactly = 1) { localRepository.deleteUploadRecord(mediaId) }
    }

    @Test
    fun `saveUploadRecords is not called for pending upload record`() = runTest {
        // Arrange
        val record = createUploadRecord(syncStatus = SyncStatus.PENDING_UPLOAD)
        coEvery { localRepository.deleteUploadRecord(any()) } just runs

        // Act
        useCase(record)

        // Assert
        coVerify(exactly = 0) { localRepository.saveUploadRecords(any()) }
    }

    @Test
    fun `scheduleDelete is not called for pending upload record`() = runTest {
        // Arrange
        val record = createUploadRecord(syncStatus = SyncStatus.PENDING_UPLOAD)
        coEvery { localRepository.deleteUploadRecord(any()) } just runs

        // Act
        useCase(record)

        // Assert
        verify(exactly = 0) { deleteScheduler.scheduleDelete() }
    }

    private fun createUploadRecord(
        mediaId: MediaId = MediaId.of("media-1"),
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ): UploadRecord = UploadRecord(
        mediaId = mediaId,
        cloudStoragePath = CloudStoragePath.of("photos/media-1.jpg"),
        isDeleted = IsDeleted.of(false),
        syncStatus = syncStatus,
        mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
    )
}
