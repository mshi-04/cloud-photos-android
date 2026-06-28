package com.appvoyager.cloudphotos.data.media.repository

import app.cash.turbine.test
import com.appvoyager.cloudphotos.data.media.datasource.LocalMediaDataSource
import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaCreatedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LocalMediaRepositoryImplTest {

    private lateinit var mockDataSource: LocalMediaDataSource
    private lateinit var repository: LocalMediaRepositoryImpl

    @BeforeEach
    fun setup() {
        mockDataSource = mockk()
        repository = LocalMediaRepositoryImpl(mockDataSource)
    }

    @Test
    fun `getMediaListFlow returns flow of media list when data source provides list`() = runTest {
        // Arrange
        coEvery { mockDataSource.getLocalMediaList() } returns expectedMediaList

        // Act & Assert
        // Flow: data source result is emitted once and completes
        repository.getMediaListFlow().test {
            assertEquals(expectedMediaList, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getMediaList returns media list when data source provides list`() = runTest {
        // Arrange
        coEvery { mockDataSource.getLocalMediaList() } returns expectedMediaList

        // Act
        // Normal: data source result is returned as repository result
        val result = repository.getMediaList()

        // Assert
        assertEquals(expectedMediaList, result)
    }

    @Test
    fun `getMediaList rethrows exception when data source throws`() = runTest {
        // Arrange
        coEvery { mockDataSource.getLocalMediaList() } throws RuntimeException("data source failure")

        // Act & Assert
        // Error: data source exception is propagated by suspend API
        assertThrows<RuntimeException> {
            repository.getMediaList()
        }
    }

    @Test
    fun `getMediaListFlow rethrows exception when data source throws`() = runTest {
        // Arrange
        coEvery { mockDataSource.getLocalMediaList() } throws RuntimeException("data source failure")

        // Act & Assert
        // Error: data source exception is propagated by Flow API
        repository.getMediaListFlow().test {
            assertEquals("data source failure", awaitError().message)
        }
    }

    companion object {
        private val sampleMedia = Media(
            id = MediaId.of("1"),
            url = MediaUrl.of("http://example.com/1.jpg"),
            type = MediaType.IMAGE,
            thumbnailUrl = null,
            createdAt = MediaCreatedAt.of(1600000000000L)
        )
        private val expectedMediaList = listOf(sampleMedia)
    }
}
