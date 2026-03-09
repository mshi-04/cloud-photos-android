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

class LocalMediaRepositoryImplTest {

    private lateinit var mockDataSource: LocalMediaDataSource
    private lateinit var repository: LocalMediaRepositoryImpl

    @BeforeEach
    fun setup() {
        mockDataSource = mockk()
        repository = LocalMediaRepositoryImpl(mockDataSource)
    }

    @Test
    fun `getMediaList returns flow emitting media list from data source`() = runTest {
        // Arrange
        val expectedMediaList = listOf(
            Media(
                id = MediaId.of("1"),
                url = MediaUrl.of("http://example.com/1.jpg"),
                type = MediaType.IMAGE,
                thumbnailUrl = null,
                createdAt = MediaCreatedAt.of(1600000000000L)
            )
        )
        coEvery { mockDataSource.getLocalMediaList() } returns expectedMediaList

        // Act
        val result = repository.getMediaList().first()

        // Assert
        assertEquals(expectedMediaList, result)
    }
}