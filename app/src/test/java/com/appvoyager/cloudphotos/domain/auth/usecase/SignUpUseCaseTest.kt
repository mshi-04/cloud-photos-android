package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.testutil.signUpRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = SignUpUseCase(repository)

    @Test
    fun `invoke returns success when repository succeeds`() = runTest(StandardTestDispatcher()) {
        val request = signUpRequest()
        val expected = AuthResult.Success(Unit)
        coEvery { repository.signUp(request) } returns expected

        val actual = useCase(request)

        assertEquals(expected, actual)
        coVerify(exactly = 1) { repository.signUp(request) }
    }

    @Test
    fun `invoke returns error when repository fails`() = runTest(StandardTestDispatcher()) {
        val request = signUpRequest()
        val expected = AuthResult.Error(AuthError.UsernameAlreadyExists("already exists"))
        coEvery { repository.signUp(request) } returns expected

        val actual = useCase(request)

        assertEquals(expected, actual)
        coVerify(exactly = 1) { repository.signUp(request) }
    }
}
