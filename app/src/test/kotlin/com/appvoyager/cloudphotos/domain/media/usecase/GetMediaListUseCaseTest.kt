package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.model.CloudMedia
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.repository.MediaRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetMediaListUseCaseTest {

    private lateinit var mediaRepository: MediaRepository
    private lateinit var getMediaListUseCase: GetMediaListUseCase

    @BeforeEach
    fun setUp() {
        mediaRepository = mockk()
        getMediaListUseCase = GetMediaListUseCase(mediaRepository)
    }

    @Test
    fun `invoke returns flow of media list from repository`() = runTest {
        // Arrange
        val expectedMediaList = listOf(
            CloudMedia(id = "1", url = "url1", type = MediaType.IMAGE, createdAt = 1000L),
            CloudMedia(id = "2", url = "url2", type = MediaType.VIDEO, createdAt = 2000L)
        )
        every { mediaRepository.getMediaList() } returns flowOf(expectedMediaList)

        // Act
        val result = getMediaListUseCase().toList()

        // Assert
        assertEquals(1, result.size)
        assertEquals(expectedMediaList, result.first())
    }
}
