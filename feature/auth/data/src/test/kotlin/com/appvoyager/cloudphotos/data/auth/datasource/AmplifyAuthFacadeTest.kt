package com.appvoyager.cloudphotos.data.auth.datasource

import com.amplifyframework.auth.AuthCategoryBehavior
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class AmplifyAuthFacadeTest {

    private val auth = mockk<AuthCategoryBehavior>()
    private val facade = AmplifyAuthFacade(auth)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `signUp returns Unit when SDK succeeds`() = runTest {
        // Arrange
        val options = AuthSignUpOptions.builder().build()
        every { auth.signUp("user@example.com", "password123", options, any(), any()) } answers {
            arg<Consumer<AuthSignUpResult>>(3).accept(mockk())
        }

        // Act
        // Interaction: Forward signup credentials and options without exposing SDK results.
        val result = facade.signUp("user@example.com", "password123", options)

        // Assert
        assertEquals(Unit, result)
        verify(exactly = 1) { auth.signUp("user@example.com", "password123", options, any(), any()) }
    }

    @Test
    fun `confirmSignUp returns Unit when SDK succeeds`() = runTest {
        // Arrange
        every { auth.confirmSignUp("user@example.com", "123456", any(), any()) } answers {
            arg<Consumer<AuthSignUpResult>>(2).accept(mockk())
        }

        // Act
        // Interaction: Forward the confirmation code to the existing SDK overload.
        val result = facade.confirmSignUp("user@example.com", "123456")

        // Assert
        assertEquals(Unit, result)
        verify(exactly = 1) { auth.confirmSignUp("user@example.com", "123456", any(), any()) }
    }

    @Test
    fun `confirmResetPassword returns Unit when SDK succeeds`() = runTest {
        // Arrange
        every { auth.confirmResetPassword("user@example.com", "newPassword123", "123456", any(), any()) } answers {
            arg<Action>(3).call()
        }

        // Act
        // Interaction: Keep the password and confirmation code in the SDK argument order.
        val result = facade.confirmResetPassword("user@example.com", "newPassword123", "123456")

        // Assert
        assertEquals(Unit, result)
        verify(exactly = 1) {
            auth.confirmResetPassword("user@example.com", "newPassword123", "123456", any(), any())
        }
    }

    @Test
    fun `signIn returns SDK result when SDK succeeds`() = runTest {
        // Arrange
        val expected = mockk<AuthSignInResult>()
        every { auth.signIn("user@example.com", "password123", any(), any()) } answers {
            arg<Consumer<AuthSignInResult>>(2).accept(expected)
        }

        // Act
        // Normal: Leave SDK result interpretation to the DataSource.
        val actual = facade.signIn("user@example.com", "password123")

        // Assert
        assertSame(expected, actual)
        verify(exactly = 1) { auth.signIn("user@example.com", "password123", any(), any()) }
    }

    @Test
    fun `getCurrentUser returns SDK user when SDK succeeds`() = runTest {
        // Arrange
        val expected = mockk<AuthUser>()
        every { auth.getCurrentUser(any(), any()) } answers {
            arg<Consumer<AuthUser>>(0).accept(expected)
        }

        // Act
        // Normal: Forward the SDK user without interpreting its fields.
        val actual = facade.getCurrentUser()

        // Assert
        assertSame(expected, actual)
        verify(exactly = 1) { auth.getCurrentUser(any(), any()) }
    }

    @Test
    fun `fetchUserAttributes returns SDK attributes when SDK succeeds`() = runTest {
        // Arrange
        val expected = listOf(mockk<AuthUserAttribute>())
        every { auth.fetchUserAttributes(any(), any()) } answers {
            arg<Consumer<List<AuthUserAttribute>>>(0).accept(expected)
        }

        // Act
        // Normal: Preserve the attribute list returned by the provider callback.
        val actual = facade.fetchUserAttributes()

        // Assert
        assertSame(expected, actual)
        verify(exactly = 1) { auth.fetchUserAttributes(any(), any()) }
    }

    @Test
    fun `fetchAuthSession returns SDK session when SDK succeeds`() = runTest {
        // Arrange
        val expected = mockk<AuthSession>()
        every { auth.fetchAuthSession(any(), any()) } answers {
            arg<Consumer<AuthSession>>(0).accept(expected)
        }

        // Act
        // Normal: Leave session state interpretation to the DataSource.
        val actual = facade.fetchAuthSession()

        // Assert
        assertSame(expected, actual)
        verify(exactly = 1) { auth.fetchAuthSession(any(), any()) }
    }

    @Test
    fun `resendSignUpCode returns Unit when SDK succeeds`() = runTest {
        // Arrange
        every { auth.resendSignUpCode("user@example.com", any(), any()) } answers {
            arg<Consumer<AuthCodeDeliveryDetails>>(1).accept(mockk())
        }

        // Act
        // Interaction: Resume the command after forwarding the requested account email.
        val actual = facade.resendSignUpCode("user@example.com")

        // Assert
        assertEquals(Unit, actual)
        verify(exactly = 1) { auth.resendSignUpCode("user@example.com", any(), any()) }
    }

    @Test
    fun `resetPassword returns Unit when SDK succeeds`() = runTest {
        // Arrange
        every { auth.resetPassword("user@example.com", any(), any()) } answers {
            arg<Consumer<AuthResetPasswordResult>>(1).accept(mockk())
        }

        // Act
        // Interaction: Resume the reset command after the provider success callback.
        val actual = facade.resetPassword("user@example.com")

        // Assert
        assertEquals(Unit, actual)
        verify(exactly = 1) { auth.resetPassword("user@example.com", any(), any()) }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "signUp", "confirmSignUp", "signIn", "getCurrentUser", "fetchUserAttributes",
            "fetchAuthSession", "resendSignUpCode", "resetPassword", "confirmResetPassword"
        ]
    )
    fun `operation rethrows SDK exception when SDK error callback fires`(operation: String) = runTest {
        // Arrange
        val failure = AuthException("SDK call failed", "Retry")
        stubSdkFailure(operation, failure)

        // Act
        // Error: Preserve the provider exception for the DataSource mapper.
        val actual = assertThrows<AuthException> {
            invokeOperation(operation)
        }

        // Assert
        assertSame(failure, actual)
        verifySdkOperation(operation)
    }

    @Test
    fun `signIn rethrows CancellationException when cancelled before late success`() = runTest {
        // Arrange
        val onSuccess = slot<Consumer<AuthSignInResult>>()
        every { auth.signIn("user@example.com", "password123", capture(onSuccess), any()) } answers { }
        val pending = async { facade.signIn("user@example.com", "password123") }
        runCurrent()

        // Act
        // Coroutine: A late SDK success must not revive cancelled waiting work.
        pending.cancel()
        onSuccess.captured.accept(mockk())
        assertThrows<CancellationException> { pending.await() }

        // Assert
        assertTrue(pending.isCancelled)
        verify(exactly = 1) { auth.signIn("user@example.com", "password123", any(), any()) }
    }

    private fun stubSdkFailure(operation: String, failure: AuthException) {
        when (operation) {
            "signUp" -> every { auth.signUp("user@example.com", "password123", any(), any(), any()) } answers {
                arg<Consumer<AuthException>>(4).accept(failure)
            }
            "confirmSignUp" -> every { auth.confirmSignUp("user@example.com", "123456", any(), any()) } answers {
                arg<Consumer<AuthException>>(3).accept(failure)
            }
            "signIn" -> every { auth.signIn("user@example.com", "password123", any(), any()) } answers {
                arg<Consumer<AuthException>>(3).accept(failure)
            }
            "getCurrentUser" -> every { auth.getCurrentUser(any(), any()) } answers {
                arg<Consumer<AuthException>>(1).accept(failure)
            }
            "fetchUserAttributes" -> every { auth.fetchUserAttributes(any(), any()) } answers {
                arg<Consumer<AuthException>>(1).accept(failure)
            }
            "fetchAuthSession" -> every { auth.fetchAuthSession(any(), any()) } answers {
                arg<Consumer<AuthException>>(1).accept(failure)
            }
            "resendSignUpCode" -> every { auth.resendSignUpCode("user@example.com", any(), any()) } answers {
                arg<Consumer<AuthException>>(2).accept(failure)
            }
            "resetPassword" -> every { auth.resetPassword("user@example.com", any(), any()) } answers {
                arg<Consumer<AuthException>>(2).accept(failure)
            }
            "confirmResetPassword" -> every {
                auth.confirmResetPassword("user@example.com", "newPassword123", "123456", any(), any())
            } answers {
                arg<Consumer<AuthException>>(4).accept(failure)
            }
            else -> error("Unknown operation: $operation")
        }
    }

    private suspend fun invokeOperation(operation: String): Any = when (operation) {
        "signUp" -> facade.signUp("user@example.com", "password123", AuthSignUpOptions.builder().build())
        "confirmSignUp" -> facade.confirmSignUp("user@example.com", "123456")
        "signIn" -> facade.signIn("user@example.com", "password123")
        "getCurrentUser" -> facade.getCurrentUser()
        "fetchUserAttributes" -> facade.fetchUserAttributes()
        "fetchAuthSession" -> facade.fetchAuthSession()
        "resendSignUpCode" -> facade.resendSignUpCode("user@example.com")
        "resetPassword" -> facade.resetPassword("user@example.com")
        "confirmResetPassword" -> facade.confirmResetPassword("user@example.com", "newPassword123", "123456")
        else -> error("Unknown operation: $operation")
    }

    private fun verifySdkOperation(operation: String) {
        when (operation) {
            "signUp" -> verify(exactly = 1) {
                auth.signUp("user@example.com", "password123", any(), any(), any())
            }
            "confirmSignUp" -> verify(exactly = 1) { auth.confirmSignUp("user@example.com", "123456", any(), any()) }
            "signIn" -> verify(exactly = 1) { auth.signIn("user@example.com", "password123", any(), any()) }
            "getCurrentUser" -> verify(exactly = 1) { auth.getCurrentUser(any(), any()) }
            "fetchUserAttributes" -> verify(exactly = 1) { auth.fetchUserAttributes(any(), any()) }
            "fetchAuthSession" -> verify(exactly = 1) { auth.fetchAuthSession(any(), any()) }
            "resendSignUpCode" -> verify(exactly = 1) { auth.resendSignUpCode("user@example.com", any(), any()) }
            "resetPassword" -> verify(exactly = 1) { auth.resetPassword("user@example.com", any(), any()) }
            "confirmResetPassword" -> verify(exactly = 1) {
                auth.confirmResetPassword("user@example.com", "newPassword123", "123456", any(), any())
            }
            else -> error("Unknown operation: $operation")
        }
    }
}
