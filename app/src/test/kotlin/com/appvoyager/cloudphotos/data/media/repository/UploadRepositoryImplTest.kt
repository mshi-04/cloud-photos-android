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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UploadRepositoryImplTest {

    private val dataSource = mockk<UploadDataSource>()
    private val repository = UploadRepositoryImpl(dataSource)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `uploadMedia calls dataSource once and returns success as is`() =
        runTest(StandardTestDispatcher()) {
            // Arrange
            val request = uploadMediaRequestFixture()
            val expected = UploadResult.Success(cloudStoragePathFixture())
            coEvery { dataSource.uploadMedia(request) } returns expected

            // Act
            val actual = repository.uploadMedia(request)

            // Assert
            assertEquals(expected, actual)
            coVerify(exactly = 1) { dataSource.uploadMedia(request) }
        }

    @Test
    fun `uploadMedia returns error from dataSource as is`() =
        runTest(StandardTestDispatcher()) {
            // Arrange
            val request = uploadMediaRequestFixture()
            val expected = UploadResult.Error(UploadError.Network("connection failed"))
            coEvery { dataSource.uploadMedia(request) } returns expected

            // Act
            val actual = repository.uploadMedia(request)

            // Assert
            assertEquals(expected, actual)
            coVerify(exactly = 1) { dataSource.uploadMedia(request) }
        }
}
