package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.model.SignInStep
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.testutil.signInRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = SignInUseCase(repository)

    @Test
    fun `invoke returns signed in state when repository returns done state`() = runTest(StandardTestDispatcher()) {
        val request = signInRequest()
        val expected = AuthResult.Success(SignInState.SignedIn)
        coEvery { repository.signIn(request) } returns expected

        val actual = useCase(request)

        assertEquals(expected, actual)
        coVerify(exactly = 1) { repository.signIn(request) }
    }

    @Test
    fun `invoke returns mfa state when repository requires mfa`() = runTest(StandardTestDispatcher()) {
        val request = signInRequest()
        val expected = AuthResult.Success(
            SignInState.MFARequired(SignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE)
        )
        coEvery { repository.signIn(request) } returns expected

        val actual = useCase(request)

        assertEquals(expected, actual)
        coVerify(exactly = 1) { repository.signIn(request) }
    }

    @Test
    fun `invoke returns error when repository fails`() = runTest(StandardTestDispatcher()) {
        val request = signInRequest()
        val expected = AuthResult.Error(AuthError.InvalidCredentials("invalid"))
        coEvery { repository.signIn(request) } returns expected

        val actual = useCase(request)

        assertEquals(expected, actual)
        coVerify(exactly = 1) { repository.signIn(request) }
    }
}
