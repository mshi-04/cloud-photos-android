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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PrepareUploadQueueUseCaseTest {

    private val localMediaRepository = mockk<LocalMediaRepository>()
    private val localUploadRecordsRepository = mockk<LocalUploadRecordsRepository>()
    private val uploadScheduler = mockk<UploadScheduler>()
    private val clock = mockk<Clock>()

    private lateinit var useCase: PrepareUploadQueueUseCase

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        useCase = PrepareUploadQueueUseCase(
            localMediaRepository = localMediaRepository,
            localUploadRecordsRepository = localUploadRecordsRepository,
            uploadScheduler = uploadScheduler,
            clock = clock
        )
    }

    @Test
    fun `invoke sets pending upload records when local media has no upload records`() = runTest {
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

    @ParameterizedTest(name = "{displayName} [{0}, {1}]")
    @CsvSource("900, 1", "901, 2")
    fun `invoke calls bounded lookups when media count reaches chunk boundary`(mediaCount: Int, expectedLookups: Int) =
        runTest {
            // Arrange
            val mediaList = (1..mediaCount).map { createMedia("media-$it") }
            val lookupIds = mutableListOf<List<MediaId>>()
            coEvery { localMediaRepository.getMediaList() } returns mediaList
            coEvery { localUploadRecordsRepository.getUploadRecords(capture(lookupIds)) } returns emptyList()
            every { clock.getCurrentTime() } returns 1700000000000L
            coEvery { localUploadRecordsRepository.saveUploadRecords(any()) } just runs
            every { uploadScheduler.scheduleUpload() } just runs

            // Act
            // Boundary: Keep every lookup within 900 IDs without losing IDs at the split.
            useCase()

            // Assert
            assertEquals(expectedLookups, lookupIds.size)
            assertTrue(lookupIds.all { it.size in 1..900 })
            assertEquals(mediaList.map { it.id }, lookupIds.flatten())
        }

    @Test
    fun `invoke sets only new records when existing records span three chunks`() = runTest {
        // Arrange
        val mediaList = (1..1801).map { createMedia("media-$it") }
        val firstIds = mediaList.take(900).map { it.id }
        val secondIds = mediaList.drop(900).take(900).map { it.id }
        val lastIds = listOf(mediaList.last().id)
        val existingIds = listOf(1, 900, 901, 1800, 1801).map { MediaId.of("media-$it") }.toSet()
        val savedRecords = slot<List<UploadRecord>>()
        coEvery { localMediaRepository.getMediaList() } returns mediaList
        coEvery { localUploadRecordsRepository.getUploadRecords(any()) } coAnswers {
            firstArg<List<MediaId>>().filter { it in existingIds }.map(::createUploadRecord)
        }
        every { clock.getCurrentTime() } returns 1700000000000L
        coEvery { localUploadRecordsRepository.saveUploadRecords(capture(savedRecords)) } just runs
        every { uploadScheduler.scheduleUpload() } just runs

        // Act
        // Interaction: Finish every lookup before saving new records once and scheduling once.
        useCase()

        // Assert
        assertEquals(
            mediaList.filter { it.id !in existingIds }.map { it.id },
            savedRecords.captured.map { it.mediaId }
        )
        coVerifyOrder {
            localUploadRecordsRepository.getUploadRecords(firstIds)
            localUploadRecordsRepository.getUploadRecords(secondIds)
            localUploadRecordsRepository.getUploadRecords(lastIds)
            localUploadRecordsRepository.saveUploadRecords(any())
            uploadScheduler.scheduleUpload()
        }
        coVerify(exactly = 3) { localUploadRecordsRepository.getUploadRecords(any()) }
        coVerify(exactly = 1) { localUploadRecordsRepository.saveUploadRecords(any()) }
        verify(exactly = 1) { uploadScheduler.scheduleUpload() }
    }

    @Test
    fun `invoke ignores writes when all media across chunks already have records`() = runTest {
        // Arrange
        val mediaList = (1..901).map { createMedia("media-$it") }
        coEvery { localMediaRepository.getMediaList() } returns mediaList
        coEvery { localUploadRecordsRepository.getUploadRecords(any()) } coAnswers {
            firstArg<List<MediaId>>().map(::createUploadRecord)
        }

        // Act
        // Boundary: Existing records in later chunks must not be queued again.
        useCase()

        // Assert
        coVerify(exactly = 2) { localUploadRecordsRepository.getUploadRecords(any()) }
        coVerify(exactly = 0) { localUploadRecordsRepository.saveUploadRecords(any()) }
        verify(exactly = 0) { uploadScheduler.scheduleUpload() }
    }

    @Test
    fun `invoke rethrows lookup failure when a later chunk fails`() = runTest {
        // Arrange
        val mediaList = (1..901).map { createMedia("media-$it") }
        val firstIds = mediaList.take(900).map { it.id }
        val lastIds = listOf(mediaList.last().id)
        val failure = IllegalStateException("Lookup failed")
        coEvery { localMediaRepository.getMediaList() } returns mediaList
        coEvery { localUploadRecordsRepository.getUploadRecords(firstIds) } returns emptyList()
        coEvery { localUploadRecordsRepository.getUploadRecords(lastIds) } throws failure

        // Act
        // Error: A later lookup failure must not leave a partially prepared queue.
        val actual = assertThrows<IllegalStateException> { useCase() }

        // Assert
        assertSame(failure, actual)
        coVerify(exactly = 1) { localUploadRecordsRepository.getUploadRecords(lastIds) }
        coVerify(exactly = 0) { localUploadRecordsRepository.saveUploadRecords(any()) }
        verify(exactly = 0) { uploadScheduler.scheduleUpload() }
    }

    @Test
    fun `invoke rethrows CancellationException when a later chunk is cancelled`() = runTest {
        // Arrange
        val mediaList = (1..901).map { createMedia("media-$it") }
        val firstIds = mediaList.take(900).map { it.id }
        val lastIds = listOf(mediaList.last().id)
        val cancellation = CancellationException("Lookup cancelled")
        coEvery { localMediaRepository.getMediaList() } returns mediaList
        coEvery { localUploadRecordsRepository.getUploadRecords(firstIds) } returns emptyList()
        coEvery { localUploadRecordsRepository.getUploadRecords(lastIds) } throws cancellation

        // Act
        // Coroutine: Preserve cancellation without saving or scheduling partial results.
        val actual = assertThrows<CancellationException> { useCase() }

        // Assert
        assertSame(cancellation, actual)
        coVerify(exactly = 1) { localUploadRecordsRepository.getUploadRecords(lastIds) }
        coVerify(exactly = 0) { localUploadRecordsRepository.saveUploadRecords(any()) }
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
