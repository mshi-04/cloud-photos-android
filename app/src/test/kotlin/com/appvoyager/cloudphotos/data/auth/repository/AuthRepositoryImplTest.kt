package com.appvoyager.cloudphotos.data.auth.repository

import com.appvoyager.cloudphotos.data.auth.testutil.authSessionFixture
import com.appvoyager.cloudphotos.data.auth.testutil.authUserFixture
import com.appvoyager.cloudphotos.data.auth.testutil.confirmSignUpRequestFixture
import com.appvoyager.cloudphotos.data.auth.testutil.signInRequestFixture
import com.appvoyager.cloudphotos.data.auth.testutil.signUpRequestFixture
import com.appvoyager.cloudphotos.data.auth.datasource.AuthDataSource
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.model.SignInStep
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private val dataSource = mockk<AuthDataSource>()
    private val repository = AuthRepositoryImpl(dataSource)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `signUp calls dataSource once and returns success as is`() =
        runTest(StandardTestDispatcher()) {
            // Arrange
            val request = signUpRequestFixture()
            val expected = AuthResult.Success(Unit)
            coEvery { dataSource.signUp(request) } returns expected

            // Act
            val actual = repository.signUp(request)

            // Assert
            assertEquals(expected, actual)
            coVerify(exactly = 1) { dataSource.signUp(request) }
        }

    @Test
    fun `confirmSignUp calls dataSource once and returns error as is`() =
        runTest(StandardTestDispatcher()) {
            // Arrange
            val request = confirmSignUpRequestFixture()
            val expected = AuthResult.Error(AuthError.CodeMismatch("invalid code"))
            coEvery { dataSource.confirmSignUp(request) } returns expected

            // Act
            val actual = repository.confirmSignUp(request)

            // Assert
            assertEquals(expected, actual)
            coVerify(exactly = 1) { dataSource.confirmSignUp(request) }
        }

    @Test
    fun `signIn delegates only to dataSource signIn and returns mapped state from dataSource`() =
        runTest(StandardTestDispatcher()) {
            // Arrange
            val request = signInRequestFixture()
            val expected = AuthResult.Success(
                SignInState.MFARequired(SignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE)
            )
            coEvery { dataSource.signIn(request) } returns expected

            // Act
            val actual = repository.signIn(request)

            // Assert
            assertEquals(expected, actual)
            coVerify(exactly = 1) { dataSource.signIn(request) }
            coVerify(exactly = 0) { dataSource.signOut() }
        }

    @Test
    fun `signOut returns error from dataSource without fallback`() =
        runTest(StandardTestDispatcher()) {
            // Arrange
            val expected = AuthResult.Error(AuthError.Network("network down"))
            coEvery { dataSource.signOut() } returns expected

            // Act
            val actual = repository.signOut()

            // Assert
            assertEquals(expected, actual)
            coVerify(exactly = 1) { dataSource.signOut() }
        }

    @Test
    fun `fetchCurrentUser returns domain user from dataSource as is`() =
        runTest(StandardTestDispatcher()) {
            // Arrange
            val expected = AuthResult.Success(authUserFixture())
            coEvery { dataSource.fetchCurrentUser() } returns expected

            // Act
            val actual = repository.fetchCurrentUser()

            // Assert
            assertEquals(expected, actual)
            coVerify(exactly = 1) { dataSource.fetchCurrentUser() }
        }

    @Test
    fun `getSession returns session from dataSource as is`() = runTest(StandardTestDispatcher()) {
        // Arrange
        val expected = AuthResult.Success(authSessionFixture())
        coEvery { dataSource.getSession() } returns expected

        // Act
        val actual = repository.getSession()

        // Assert
        assertEquals(expected, actual)
        coVerify(exactly = 1) { dataSource.getSession() }
    }
}
