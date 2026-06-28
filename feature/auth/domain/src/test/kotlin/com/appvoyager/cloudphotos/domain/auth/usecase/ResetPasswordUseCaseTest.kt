package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.request.ResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ResetPasswordUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private lateinit var useCase: ResetPasswordUseCase

    @BeforeEach
    fun setUp() {
        useCase = ResetPasswordUseCase(repository)
    }

    @Test
    fun `invoke returns Success when repository succeeds`() = runTest {
        // Arrange
        val request = ResetPasswordRequest(Email.of("test@example.com"))
        val expected = AuthResult.Success(Unit)
        coEvery { repository.resetPassword(request) } returns expected

        // Act
        // Normal: repository success is returned unchanged
        val result = useCase(request)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun `invoke calls resetPassword with given request when invoked`() = runTest {
        // Arrange
        val request = ResetPasswordRequest(Email.of("test@example.com"))
        coEvery { repository.resetPassword(request) } returns AuthResult.Success(Unit)

        // Act
        // Interaction: use case delegates to repository with the same request
        useCase(request)

        // Assert
        coVerify(exactly = 1) { repository.resetPassword(request) }
    }

    @Test
    fun `invoke returns Error when repository fails`() = runTest {
        // Arrange
        val request = ResetPasswordRequest(Email.of("test@example.com"))
        val expected = AuthResult.Error(AuthError.Network())
        coEvery { repository.resetPassword(request) } returns expected

        // Act
        // Error: repository error is returned unchanged
        val result = useCase(request)

        // Assert
        assertEquals(expected, result)
    }
}
