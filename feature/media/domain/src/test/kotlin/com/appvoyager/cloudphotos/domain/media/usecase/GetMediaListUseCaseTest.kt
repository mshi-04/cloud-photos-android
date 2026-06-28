package com.appvoyager.cloudphotos.domain.media.usecase

import app.cash.turbine.test
import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.repository.LocalMediaRepository
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaCreatedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetMediaListUseCaseTest {

    private lateinit var localMediaRepository: LocalMediaRepository
    private lateinit var getMediaListUseCase: GetMediaListUseCase

    @BeforeEach
    fun setUp() {
        localMediaRepository = mockk()
        getMediaListUseCase = GetMediaListUseCase(localMediaRepository)
    }

    @Test
    fun `invoke returns flow of media list when repository provides list`() = runTest {
        // Arrange
        val expectedMediaList = listOf(
            Media(
                id = MediaId.of("1"),
                url = MediaUrl.of("content://media/external/images/media/1"),
                type = MediaType.IMAGE,
                thumbnailUrl = null,
                createdAt = MediaCreatedAt.of(1600000000000L)
            )
        )
        every { localMediaRepository.getMediaListFlow() } returns MutableStateFlow(expectedMediaList)

        // Act & Assert
        // Flow: repository media list is emitted without requiring completion
        getMediaListUseCase().test {
            assertEquals(expectedMediaList, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `invoke rethrows exception when repository throws`() = runTest {
        // Arrange
        val expected = RuntimeException("repository failure")
        every { localMediaRepository.getMediaListFlow() } returns flow { throw expected }

        // Act & Assert
        // Error: repository flow failure is propagated to the collector
        getMediaListUseCase().test {
            val actual = awaitError()
            assertEquals(expected, actual)
        }
    }
}
