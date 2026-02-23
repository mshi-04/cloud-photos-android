package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.testutil.authUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FetchCurrentUserUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = FetchCurrentUserUseCase(repository)

    @Test
    fun `invoke returns current user when repository succeeds`() =
        runTest(StandardTestDispatcher()) {
            val expected = AuthResult.Success(authUser())
            coEvery { repository.fetchCurrentUser() } returns expected

            val actual = useCase()

            assertEquals(expected, actual)
            coVerify(exactly = 1) { repository.fetchCurrentUser() }
        }

    @Test
    fun `invoke returns error when repository fails`() = runTest(StandardTestDispatcher()) {
        val expected = AuthResult.Error(AuthError.Unknown("unexpected"))
        coEvery { repository.fetchCurrentUser() } returns expected

        val actual = useCase()

        assertEquals(expected, actual)
        coVerify(exactly = 1) { repository.fetchCurrentUser() }
    }
}
