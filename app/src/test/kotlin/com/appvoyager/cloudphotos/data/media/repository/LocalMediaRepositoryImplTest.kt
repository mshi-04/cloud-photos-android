package com.appvoyager.cloudphotos.data.media.repository

import com.appvoyager.cloudphotos.data.media.datasource.LocalMediaDataSource
import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaCreatedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
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
    fun `getMediaListFlow returns flow emitting media list from data source`() = runTest {
        // Arrange
        coEvery { mockDataSource.getLocalMediaList() } returns expectedMediaList

        // Act
        val result = repository.getMediaListFlow().first()

        // Assert
        assertEquals(expectedMediaList, result)
    }

    @Test
    fun `getMediaList returns media list from data source`() = runTest {
        // Arrange
        coEvery { mockDataSource.getLocalMediaList() } returns expectedMediaList

        // Act
        val result = repository.getMediaList()

        // Assert
        assertEquals(expectedMediaList, result)
    }

    @Test
    fun `getMediaList propagates exception from data source`() = runTest {
        // Arrange
        coEvery { mockDataSource.getLocalMediaList() } throws RuntimeException("data source failure")

        // Act & Assert
        assertThrows<RuntimeException> {
            repository.getMediaList()
        }
    }

    @Test
    fun `getMediaListFlow propagates exception from data source`() = runTest {
        // Arrange
        coEvery { mockDataSource.getLocalMediaList() } throws RuntimeException("data source failure")

        // Act & Assert
        assertThrows<RuntimeException> {
            repository.getMediaListFlow().first()
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
        val expectedMediaList = listOf(sampleMedia)
    }
}