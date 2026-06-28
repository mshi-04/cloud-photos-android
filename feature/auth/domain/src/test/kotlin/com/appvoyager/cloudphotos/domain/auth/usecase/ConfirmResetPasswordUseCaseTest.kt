package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.valueobject.ConfirmationCode
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.Password
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConfirmResetPasswordUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private lateinit var useCase: ConfirmResetPasswordUseCase

    @BeforeEach
    fun setUp() {
        useCase = ConfirmResetPasswordUseCase(repository)
    }

    @Test
    fun `invoke returns Success when repository succeeds`() = runTest {
        // Arrange
        val request = createRequest()
        val expected = AuthResult.Success(Unit)
        coEvery { repository.confirmResetPassword(request) } returns expected

        // Act
        // Normal: repository success is returned unchanged
        val result = useCase(request)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun `invoke calls confirmResetPassword with given request when invoked`() = runTest {
        // Arrange
        val request = createRequest()
        coEvery { repository.confirmResetPassword(request) } returns AuthResult.Success(Unit)

        // Act
        // Interaction: use case delegates to repository with the same request
        useCase(request)

        // Assert
        coVerify(exactly = 1) { repository.confirmResetPassword(request) }
    }

    @Test
    fun `invoke returns Error when repository fails`() = runTest {
        // Arrange
        val request = createRequest()
        val expected = AuthResult.Error(AuthError.CodeMismatch())
        coEvery { repository.confirmResetPassword(request) } returns expected

        // Act
        // Error: repository error is returned unchanged
        val result = useCase(request)

        // Assert
        assertEquals(expected, result)
    }

    private fun createRequest() = ConfirmResetPasswordRequest(
        email = Email.of("test@example.com"),
        code = ConfirmationCode.of("123456"),
        newPassword = Password.of("password1")
    )
}
