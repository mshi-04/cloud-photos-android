package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.UploadScheduler
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
    fun `invoke sets correct mediaId when invoked`() = runTest {
        // Arrange
        val mediaId = MediaId.of("media-1")
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs
        every { uploadScheduler.scheduleUpload() } just runs

        // Act
        useCase(mediaId, MediaUploadedAt.of(1700000000000L))

        // Assert
        assertEquals(mediaId, slot.captured.first().mediaId)
    }

    @Test
    fun `invoke sets cloudStoragePath to null when invoked`() = runTest {
        // Arrange
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs
        every { uploadScheduler.scheduleUpload() } just runs

        // Act
        useCase(MediaId.of("media-1"), MediaUploadedAt.of(1700000000000L))

        // Assert
        assertNull(slot.captured.first().cloudStoragePath)
    }

    @Test
    fun `invoke sets isDeleted to false when invoked`() = runTest {
        // Arrange
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs
        every { uploadScheduler.scheduleUpload() } just runs

        // Act
        useCase(MediaId.of("media-1"), MediaUploadedAt.of(1700000000000L))

        // Assert
        assertEquals(IsDeleted.of(false), slot.captured.first().isDeleted)
    }

    @Test
    fun `invoke sets syncStatus to PENDING_UPLOAD when invoked`() = runTest {
        // Arrange
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs
        every { uploadScheduler.scheduleUpload() } just runs

        // Act
        useCase(MediaId.of("media-1"), MediaUploadedAt.of(1700000000000L))

        // Assert
        assertEquals(SyncStatus.PENDING_UPLOAD, slot.captured.first().syncStatus)
    }

    @Test
    fun `invoke sets mediaUploadedAt to provided value when invoked`() = runTest {
        // Arrange
        val uploadedAt = MediaUploadedAt.of(1700000000000L)
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs
        every { uploadScheduler.scheduleUpload() } just runs

        // Act
        useCase(MediaId.of("media-1"), uploadedAt)

        // Assert
        assertEquals(uploadedAt, slot.captured.first().mediaUploadedAt)
    }

    @Test
    fun `invoke calls scheduleUpload when invoked`() = runTest {
        // Arrange
        coEvery { localRepository.saveUploadRecords(any()) } just runs
        every { uploadScheduler.scheduleUpload() } just runs

        // Act
        useCase(MediaId.of("media-1"), MediaUploadedAt.of(1700000000000L))

        // Assert
        verify { uploadScheduler.scheduleUpload() }
    }
}
