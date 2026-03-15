package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.UploadScheduler
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RecordMediaUploadUseCaseTest {

    private val localRepository = mockk<LocalUploadRecordsRepository>()
    private val uploadScheduler = mockk<UploadScheduler>()

    private lateinit var useCase: RecordMediaUploadUseCase

    @BeforeEach
    fun setUp() {
        useCase = RecordMediaUploadUseCase(localRepository, uploadScheduler)
    }

    @Test
    fun `saved record has correct mediaId`() = runTest {
        // Arrange
        val mediaId = MediaId.of("media-1")
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs
        coEvery { uploadScheduler.scheduleUpload(any()) } just runs

        // Act
        useCase(
            mediaId,
            CloudStoragePath.of("photos/media-1.jpg"),
            MediaUploadedAt.of(1700000000000L)
        )

        // Assert
        assertEquals(mediaId, slot.captured.first().mediaId)
    }

    @Test
    fun `saved record has isDeleted set to false`() = runTest {
        // Arrange
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs
        coEvery { uploadScheduler.scheduleUpload(any()) } just runs

        // Act
        useCase(
            MediaId.of("media-1"),
            CloudStoragePath.of("photos/media-1.jpg"),
            MediaUploadedAt.of(1700000000000L)
        )

        // Assert
        assertEquals(IsDeleted.of(false), slot.captured.first().isDeleted)
    }

    @Test
    fun `saved record has syncStatus set to PENDING_UPLOAD`() = runTest {
        // Arrange
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs
        coEvery { uploadScheduler.scheduleUpload(any()) } just runs

        // Act
        useCase(
            MediaId.of("media-1"),
            CloudStoragePath.of("photos/media-1.jpg"),
            MediaUploadedAt.of(1700000000000L)
        )

        // Assert
        assertEquals(SyncStatus.PENDING_UPLOAD, slot.captured.first().syncStatus)
    }

    @Test
    fun `saved record has the provided mediaUploadedAt`() = runTest {
        // Arrange
        val uploadedAt = MediaUploadedAt.of(1700000000000L)
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs
        coEvery { uploadScheduler.scheduleUpload(any()) } just runs

        // Act
        useCase(MediaId.of("media-1"), CloudStoragePath.of("photos/media-1.jpg"), uploadedAt)

        // Assert
        assertEquals(uploadedAt, slot.captured.first().mediaUploadedAt)
    }

    @Test
    fun `scheduleUpload is called with the correct mediaId`() = runTest {
        // Arrange
        val mediaId = MediaId.of("media-1")
        coEvery { localRepository.saveUploadRecords(any()) } just runs
        coEvery { uploadScheduler.scheduleUpload(any()) } just runs

        // Act
        useCase(
            mediaId,
            CloudStoragePath.of("photos/media-1.jpg"),
            MediaUploadedAt.of(1700000000000L)
        )

        // Assert
        verify { uploadScheduler.scheduleUpload(mediaId) }
    }
}
