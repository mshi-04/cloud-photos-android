package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignOutUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = SignOutUseCase(repository)

    @Test
    fun `invoke returns success when repository succeeds`() = runTest(StandardTestDispatcher()) {
        val expected = AuthResult.Success(Unit)
        coEvery { repository.signOut() } returns expected

        val actual = useCase()

        assertEquals(expected, actual)
        coVerify(exactly = 1) { repository.signOut() }
    }

    @Test
    fun `invoke returns error when repository fails`() = runTest(StandardTestDispatcher()) {
        val expected = AuthResult.Error(AuthError.Network("offline"))
        coEvery { repository.signOut() } returns expected

        val actual = useCase()

        assertEquals(expected, actual)
        coVerify(exactly = 1) { repository.signOut() }
    }
}
