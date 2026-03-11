package com.appvoyager.cloudphotos.domain.upload.usecase

import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import com.appvoyager.cloudphotos.domain.upload.model.UploadError
import com.appvoyager.cloudphotos.domain.upload.model.UploadResult
import com.appvoyager.cloudphotos.domain.upload.model.errorOrNull
import com.appvoyager.cloudphotos.domain.upload.model.getOrNull
import com.appvoyager.cloudphotos.domain.upload.repository.UploadRepository
import com.appvoyager.cloudphotos.domain.upload.request.UploadMediaRequest
import com.appvoyager.cloudphotos.domain.upload.valueobject.ContentType
import com.appvoyager.cloudphotos.domain.upload.valueobject.StoragePath
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UploadMediaUseCaseTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var uploadRepository: UploadRepository
    private lateinit var uploadMediaUseCase: UploadMediaUseCase

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        uploadRepository = mockk()
        uploadMediaUseCase = UploadMediaUseCase(uploadRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invoke returns success when repository succeeds`() = runTest {
        // Arrange
        val request = UploadMediaRequest(
            localUri = MediaUrl.of("content://media/external/images/media/1"),
            contentType = ContentType.of("image/jpeg")
        )
        val expectedPath = StoragePath.of("photos/user123/image.jpg")
        coEvery { uploadRepository.uploadMedia(request) } returns UploadResult.Success(expectedPath)

        // Act
        val result = uploadMediaUseCase(request)

        // Assert
        assertEquals(expectedPath, result.getOrNull())
    }

    @Test
    fun `invoke returns error when repository fails`() = runTest {
        // Arrange
        val request = UploadMediaRequest(
            localUri = MediaUrl.of("content://media/external/images/media/1"),
            contentType = ContentType.of("image/jpeg")
        )
        val expectedError = UploadError.Network("connection failed")
        coEvery { uploadRepository.uploadMedia(request) } returns UploadResult.Error(expectedError)

        // Act
        val result = uploadMediaUseCase(request)

        // Assert
        assertEquals(expectedError, result.errorOrNull())
    }
}
