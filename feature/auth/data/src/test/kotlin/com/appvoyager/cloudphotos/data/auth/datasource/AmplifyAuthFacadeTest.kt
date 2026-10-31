package com.appvoyager.cloudphotos.data.auth.datasource

import com.amplifyframework.auth.AuthCategoryBehavior
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.options.AuthSignUpOptions
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
    fun `signIn rethrows SDK exception when SDK fails`() = runTest {
        // Arrange
        val failure = AuthException("Sign in failed", "Retry")
        every { auth.signIn("user@example.com", "password123", any(), any()) } answers {
            arg<Consumer<AuthException>>(3).accept(failure)
        }

        // Act
        // Error: Preserve the provider exception for the DataSource mapper.
        val actual = assertThrows<AuthException> {
            facade.signIn("user@example.com", "password123")
        }

        // Assert
        assertSame(failure, actual)
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
}
