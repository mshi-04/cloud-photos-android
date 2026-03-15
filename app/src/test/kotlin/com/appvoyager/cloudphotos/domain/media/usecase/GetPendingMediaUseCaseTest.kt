package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.repository.LocalMediaRepository
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaCreatedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetPendingMediaUseCaseTest {

    private val localMediaRepository = mockk<LocalMediaRepository>()
    private val localUploadRecordsRepository = mockk<LocalUploadRecordsRepository>()

    private lateinit var useCase: GetPendingMediaUseCase

    @BeforeEach
    fun setUp() {
        useCase = GetPendingMediaUseCase(localMediaRepository, localUploadRecordsRepository)
    }

    @Test
    fun `returns all media when no upload records exist`() = runTest {
        // Arrange
        val media1 = createMedia("1")
        val media2 = createMedia("2")
        coEvery { localMediaRepository.getMediaList() } returns listOf(media1, media2)
        coEvery { localUploadRecordsRepository.getUploadRecords(any()) } returns emptyList()

        // Act
        val result = useCase()

        // Assert
        assertEquals(listOf(media1, media2), result)
    }

    @Test
    fun `returns empty list when all media have upload records`() = runTest {
        // Arrange
        val media1 = createMedia("1")
        val media2 = createMedia("2")
        coEvery { localMediaRepository.getMediaList() } returns listOf(media1, media2)
        coEvery { localUploadRecordsRepository.getUploadRecords(any()) } returns listOf(
            createUploadRecord("1"),
            createUploadRecord("2")
        )

        // Act
        val result = useCase()

        // Assert
        assertEquals(emptyList<Media>(), result)
    }

    @Test
    fun `returns only media without upload records`() = runTest {
        // Arrange
        val media1 = createMedia("1")
        val media2 = createMedia("2")
        val media3 = createMedia("3")
        coEvery { localMediaRepository.getMediaList() } returns listOf(media1, media2, media3)
        coEvery { localUploadRecordsRepository.getUploadRecords(any()) } returns listOf(
            createUploadRecord("1"),
            createUploadRecord("3")
        )

        // Act
        val result = useCase()

        // Assert
        assertEquals(listOf(media2), result)
    }

    @Test
    fun `returns empty list when no local media exist`() = runTest {
        // Arrange
        coEvery { localMediaRepository.getMediaList() } returns emptyList()
        coEvery { localUploadRecordsRepository.getUploadRecords(any()) } returns emptyList()

        // Act
        val result = useCase()

        // Assert
        assertEquals(emptyList<Media>(), result)
    }

    private fun createMedia(id: String): Media = Media(
        id = MediaId.of(id),
        url = MediaUrl.of("content://media/external/images/media/$id"),
        type = MediaType.IMAGE,
        thumbnailUrl = null,
        createdAt = MediaCreatedAt.of(1600000000000L)
    )

    private fun createUploadRecord(id: String): UploadRecord = UploadRecord(
        mediaId = MediaId.of(id),
        cloudStoragePath = CloudStoragePath.of("photos/$id.jpg"),
        isDeleted = IsDeleted.of(false),
        syncStatus = SyncStatus.SYNCED,
        mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
    )
}
