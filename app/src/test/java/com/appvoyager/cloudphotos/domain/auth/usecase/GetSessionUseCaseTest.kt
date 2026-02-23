package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.testutil.guestSession
import com.appvoyager.cloudphotos.domain.auth.testutil.signedInSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetSessionUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = GetSessionUseCase(repository)

    @Test
    fun `invoke returns signed in session when repository succeeds`() =
        runTest(StandardTestDispatcher()) {
            val expected = AuthResult.Success(signedInSession())
            coEvery { repository.getSession() } returns expected

            val actual = useCase()

            assertEquals(expected, actual)
            coVerify(exactly = 1) { repository.getSession() }
        }

    @Test
    fun `invoke returns guest session when repository returns guest`() =
        runTest(StandardTestDispatcher()) {
            val expected = AuthResult.Success(guestSession())
            coEvery { repository.getSession() } returns expected

            val actual = useCase()

            assertEquals(expected, actual)
            coVerify(exactly = 1) { repository.getSession() }
        }

    @Test
    fun `invoke returns error when repository fails`() = runTest(StandardTestDispatcher()) {
        val expected = AuthResult.Error(AuthError.Network("network"))
        coEvery { repository.getSession() } returns expected

        val actual = useCase()

        assertEquals(expected, actual)
        coVerify(exactly = 1) { repository.getSession() }
    }
}
