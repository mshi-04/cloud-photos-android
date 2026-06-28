package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.common.Clock
import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.repository.LocalMediaRepository
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.UploadScheduler
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaCreatedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
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

class PrepareUploadQueueUseCaseTest {

    private val localMediaRepository = mockk<LocalMediaRepository>()
    private val localUploadRecordsRepository = mockk<LocalUploadRecordsRepository>()
    private val uploadScheduler = mockk<UploadScheduler>()
    private val clock = mockk<Clock>()

    private lateinit var useCase: PrepareUploadQueueUseCase

    @BeforeEach
    fun setUp() {
        useCase = PrepareUploadQueueUseCase(
            localMediaRepository = localMediaRepository,
            localUploadRecordsRepository = localUploadRecordsRepository,
            uploadScheduler = uploadScheduler,
            clock = clock
        )
    }

    @Test
    fun `invoke saves pending upload records when local media has no upload records`() = runTest {
        // Arrange
        val media = createMedia("media-1")
        val slot = slot<List<UploadRecord>>()
        coEvery { localMediaRepository.getMediaList() } returns listOf(media)
        coEvery { localUploadRecordsRepository.getUploadRecords(listOf(media.id)) } returns emptyList()
        every { clock.getCurrentTime() } returns 1700000000000L
        coEvery { localUploadRecordsRepository.saveUploadRecords(capture(slot)) } just runs
        every { uploadScheduler.scheduleUpload() } just runs

        // Act
        // Normal: new local media is converted to pending upload record
        useCase()

        // Assert
        assertEquals(
            PendingRecordSnapshot(
                mediaId = media.id,
                isDeleted = IsDeleted.of(false),
                syncStatus = SyncStatus.PENDING_UPLOAD,
                mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
            ),
            PendingRecordSnapshot.from(slot.captured.single())
        )
    }

    @Test
    fun `invoke calls scheduleUpload when new records are saved`() = runTest {
        // Arrange
        val media = createMedia("media-1")
        coEvery { localMediaRepository.getMediaList() } returns listOf(media)
        coEvery { localUploadRecordsRepository.getUploadRecords(listOf(media.id)) } returns emptyList()
        every { clock.getCurrentTime() } returns 1700000000000L
        coEvery { localUploadRecordsRepository.saveUploadRecords(any()) } just runs
        every { uploadScheduler.scheduleUpload() } just runs

        // Act
        // Interaction: saving new records schedules upload work
        useCase()

        // Assert
        verify(exactly = 1) { uploadScheduler.scheduleUpload() }
    }

    @Test
    fun `invoke ignores upload record lookup when local media is empty`() = runTest {
        // Arrange
        coEvery { localMediaRepository.getMediaList() } returns emptyList()

        // Act
        // Boundary/Interaction: empty media list exits before upload record lookup
        useCase()

        // Assert
        coVerify(exactly = 0) { localUploadRecordsRepository.getUploadRecords(any()) }
    }

    @Test
    fun `invoke ignores scheduler when local media is empty`() = runTest {
        // Arrange
        coEvery { localMediaRepository.getMediaList() } returns emptyList()

        // Act
        // Boundary/Interaction: empty media list does not schedule upload work
        useCase()

        // Assert
        verify(exactly = 0) { uploadScheduler.scheduleUpload() }
    }

    @Test
    fun `invoke ignores saveUploadRecords when every media already has upload record`() = runTest {
        // Arrange
        val media = createMedia("media-1")
        val existingRecord = createUploadRecord(media.id)
        coEvery { localMediaRepository.getMediaList() } returns listOf(media)
        coEvery { localUploadRecordsRepository.getUploadRecords(listOf(media.id)) } returns listOf(existingRecord)

        // Act
        // Boundary/Interaction: existing upload record prevents duplicate pending record
        useCase()

        // Assert
        coVerify(exactly = 0) { localUploadRecordsRepository.saveUploadRecords(any()) }
    }

    @Test
    fun `invoke ignores scheduleUpload when every media already has upload record`() = runTest {
        // Arrange
        val media = createMedia("media-1")
        val existingRecord = createUploadRecord(media.id)
        coEvery { localMediaRepository.getMediaList() } returns listOf(media)
        coEvery { localUploadRecordsRepository.getUploadRecords(listOf(media.id)) } returns listOf(existingRecord)

        // Act
        // Boundary/Interaction: existing upload record prevents scheduling duplicate upload work
        useCase()

        // Assert
        verify(exactly = 0) { uploadScheduler.scheduleUpload() }
    }

    private fun createMedia(id: String) = Media(
        id = MediaId.of(id),
        url = MediaUrl.of("content://media/$id"),
        type = MediaType.IMAGE,
        createdAt = MediaCreatedAt.of(1700000000000L)
    )

    private fun createUploadRecord(mediaId: MediaId) = UploadRecord(
        mediaId = mediaId,
        cloudStoragePath = CloudStoragePath.of("private/identity/${mediaId.value}.jpg"),
        isDeleted = IsDeleted.of(false),
        syncStatus = SyncStatus.SYNCED,
        mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
    )

    private data class PendingRecordSnapshot(
        val mediaId: MediaId,
        val isDeleted: IsDeleted,
        val syncStatus: SyncStatus,
        val mediaUploadedAt: MediaUploadedAt
    ) {
        companion object {
            fun from(record: UploadRecord) = PendingRecordSnapshot(
                mediaId = record.mediaId,
                isDeleted = record.isDeleted,
                syncStatus = record.syncStatus,
                mediaUploadedAt = record.mediaUploadedAt
            )
        }
    }
}
