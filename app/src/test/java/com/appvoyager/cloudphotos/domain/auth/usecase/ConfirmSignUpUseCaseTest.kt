package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.testutil.confirmSignUpRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmSignUpUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = ConfirmSignUpUseCase(repository)

    @Test
    fun `invoke returns success when repository succeeds`() = runTest(StandardTestDispatcher()) {
        val request = confirmSignUpRequest()
        val expected = AuthResult.Success(Unit)
        coEvery { repository.confirmSignUp(request) } returns expected

        val actual = useCase(request)

        assertEquals(expected, actual)
        coVerify(exactly = 1) { repository.confirmSignUp(request) }
    }

    @Test
    fun `invoke returns error when repository fails`() = runTest(StandardTestDispatcher()) {
        val request = confirmSignUpRequest()
        val expected = AuthResult.Error(AuthError.CodeMismatch("mismatch"))
        coEvery { repository.confirmSignUp(request) } returns expected

        val actual = useCase(request)

        assertEquals(expected, actual)
        coVerify(exactly = 1) { repository.confirmSignUp(request) }
    }
}
