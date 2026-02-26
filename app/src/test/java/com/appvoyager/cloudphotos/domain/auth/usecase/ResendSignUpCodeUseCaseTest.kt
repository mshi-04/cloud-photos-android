package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.testutil.validEmail
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResendSignUpCodeUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = ResendSignUpCodeUseCase(repository)

    @Test
    fun `invoke returns success when repository succeeds`() =
        runTest(StandardTestDispatcher()) {
            // Arrange
            val email = validEmail()
            val expected = AuthResult.Success(Unit)
            coEvery { repository.resendSignUpCode(email) } returns expected

            // Act
            val actual = useCase(email)

            // Assert
            assertEquals(expected, actual)
            coVerify(exactly = 1) { repository.resendSignUpCode(email) }
        }

    @Test
    fun `invoke returns error when repository fails`() =
        runTest(StandardTestDispatcher()) {
            // Arrange
            val email = validEmail()
            val expected = AuthResult.Error(AuthError.TooManyRequests("too many"))
            coEvery { repository.resendSignUpCode(email) } returns expected

            // Act
            val actual = useCase(email)

            // Assert
            assertEquals(expected, actual)
            coVerify(exactly = 1) { repository.resendSignUpCode(email) }
        }

}
