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
    fun `saved record has isDeleted set to true`() = runTest {
        // Arrange
        val record = createUploadRecord()
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs
        coEvery { deleteScheduler.scheduleDelete(any()) } just runs

        // Act
        useCase(record)

        // Assert
        assertEquals(IsDeleted.of(true), slot.captured.first().isDeleted)
    }

    @Test
    fun `saved record has syncStatus set to PENDING_DELETE`() = runTest {
        // Arrange
        val record = createUploadRecord()
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs
        coEvery { deleteScheduler.scheduleDelete(any()) } just runs

        // Act
        useCase(record)

        // Assert
        assertEquals(SyncStatus.PENDING_DELETE, slot.captured.first().syncStatus)
    }

    @Test
    fun `scheduleDelete is called with the record mediaId`() = runTest {
        // Arrange
        val mediaId = MediaId.of("media-1")
        val record = createUploadRecord(mediaId = mediaId)
        coEvery { localRepository.saveUploadRecords(any()) } just runs
        coEvery { deleteScheduler.scheduleDelete(any()) } just runs

        // Act
        useCase(record)

        // Assert
        verify { deleteScheduler.scheduleDelete(mediaId) }
    }

    @Test
    fun `saveUploadRecords is called once`() = runTest {
        // Arrange
        val record = createUploadRecord()
        coEvery { localRepository.saveUploadRecords(any()) } just runs
        coEvery { deleteScheduler.scheduleDelete(any()) } just runs

        // Act
        useCase(record)

        // Assert
        coVerify(exactly = 1) { localRepository.saveUploadRecords(any()) }
    }

    private fun createUploadRecord(
        mediaId: MediaId = MediaId.of("media-1")
    ): UploadRecord = UploadRecord(
        mediaId = mediaId,
        cloudStoragePath = CloudStoragePath.of("photos/media-1.jpg"),
        isDeleted = IsDeleted.of(false),
        syncStatus = SyncStatus.SYNCED,
        mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
    )
}
