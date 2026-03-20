package com.appvoyager.cloudphotos.data.media.repository

import com.appvoyager.cloudphotos.data.media.datasource.UploadDataSource
import com.appvoyager.cloudphotos.data.media.testutil.cloudStoragePathFixture
import com.appvoyager.cloudphotos.data.media.testutil.uploadMediaRequestFixture
import com.appvoyager.cloudphotos.domain.media.model.UploadError
import com.appvoyager.cloudphotos.domain.media.model.UploadResult
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UploadRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dataSource = mockk<UploadDataSource>()
    private val repository = UploadRepositoryImpl(dataSource)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uploadMedia returns success as is`() = runTest(testDispatcher) {
        // Arrange
        val request = uploadMediaRequestFixture()
        val expected = UploadResult.Success(cloudStoragePathFixture())
        coEvery { dataSource.uploadMedia(request) } returns expected

        // Act
        val actual = repository.uploadMedia(request)

        // Assert
        assertEquals(expected, actual)
    }

    @Test
    fun `uploadMedia delegates to dataSource once on success`() = runTest(testDispatcher) {
        // Arrange
        val request = uploadMediaRequestFixture()
        val expected = UploadResult.Success(cloudStoragePathFixture())
        coEvery { dataSource.uploadMedia(request) } returns expected

        // Act
        repository.uploadMedia(request)

        // Assert
        coVerify(exactly = 1) { dataSource.uploadMedia(request) }
    }

    @Test
    fun `uploadMedia returns error as is`() = runTest(testDispatcher) {
        // Arrange
        val request = uploadMediaRequestFixture()
        val expected = UploadResult.Error(UploadError.Network("connection failed"))
        coEvery { dataSource.uploadMedia(request) } returns expected

        // Act
        val actual = repository.uploadMedia(request)

        // Assert
        assertEquals(expected, actual)
    }

    @Test
    fun `uploadMedia delegates to dataSource once on error`() = runTest(testDispatcher) {
        // Arrange
        val request = uploadMediaRequestFixture()
        val expected = UploadResult.Error(UploadError.Network("connection failed"))
        coEvery { dataSource.uploadMedia(request) } returns expected

        // Act
        repository.uploadMedia(request)

        // Assert
        coVerify(exactly = 1) { dataSource.uploadMedia(request) }
    }
}
