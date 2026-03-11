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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UploadMediaUseCaseTest {

    private lateinit var uploadRepository: UploadRepository
    private lateinit var uploadMediaUseCase: UploadMediaUseCase

    @BeforeEach
    fun setUp() {
        uploadRepository = mockk()
        uploadMediaUseCase = UploadMediaUseCase(uploadRepository)
    }

    @Test
    fun `invoke returns Success when repository succeeds`() = runTest {
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
    fun `invoke returns Error when repository fails`() = runTest {
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
