package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.usecase.SignInUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.SignUpUseCase
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.ui.auth.effect.AuthSnackbarMessage
import com.appvoyager.cloudphotos.ui.auth.effect.LoginEffect
import com.appvoyager.cloudphotos.ui.auth.uistate.AuthFieldError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val signInUseCase = mockk<SignInUseCase>()
    private val signUpUseCase = mockk<SignUpUseCase>()

    private lateinit var viewModel: LoginViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(SavedStateHandle(), signInUseCase, signUpUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState sets email to empty when viewModel is initialized`() {
        // Arrange
        // State: freshly created viewModel exposes an empty login form

        // Act
        val state = viewModel.uiState.value

        // Assert
        Assertions.assertEquals(LoginFormSnapshot(), LoginFormSnapshot.from(state))
    }

    @Test
    fun `onEmailChanged sets email when called`() {
        // Arrange
        // State: email field starts empty before user input

        // Act
        // State: email input updates the form state
        viewModel.onEmailChanged("test@example.com")
        val state = viewModel.uiState.value

        // Assert
        Assertions.assertEquals(
            LoginFormSnapshot(email = "test@example.com"),
            LoginFormSnapshot.from(state)
        )
    }

    @Test
    fun `onPasswordChanged sets password when called`() {
        // Arrange
        // State: password field starts empty before user input

        // Act
        // State: password input updates the form state
        viewModel.onPasswordChanged("password1")
        val state = viewModel.uiState.value

        // Assert
        Assertions.assertEquals(
            LoginFormSnapshot(password = "password1"),
            LoginFormSnapshot.from(state)
        )
    }

    @Test
    fun `isFormValid returns true when email and password are valid`() {
        // Arrange
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password1")

        // Act
        // Normal: valid email and valid password make the form submittable
        val isFormValid = viewModel.isFormValid

        // Assert
        Assertions.assertTrue(isFormValid)
    }

    @Test
    fun `isFormValid returns false when email is invalid`() {
        // Arrange
        viewModel.onEmailChanged("invalid")
        viewModel.onPasswordChanged("password1")

        // Act
        // Boundary: invalid email prevents submit even when password is valid
        val isFormValid = viewModel.isFormValid

        // Assert
        Assertions.assertFalse(isFormValid)
    }

    @Test
    fun `isFormValid returns false when password is too short`() {
        // Arrange
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("short")

        // Act
        // Boundary: short password prevents submit even when email is valid
        val isFormValid = viewModel.isFormValid

        // Assert
        Assertions.assertFalse(isFormValid)
    }

    @Test
    fun `onSignIn emits NavigateToHome when sign in returns SignedIn`() = runTest(testDispatcher) {
        // Arrange
        // Flow: signed-in result emits home navigation effect
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password1")
        coEvery { signInUseCase(any()) } returns AuthResult.Success(SignInState.SignedIn)

        // Act & Assert
        viewModel.effect.test {
            viewModel.onSignIn()
            advanceUntilIdle()

            Assertions.assertEquals(LoginEffect.NavigateToHome, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onSignIn emits NavigateToVerification when sign in returns UserNotConfirmed error`() =
        runTest(testDispatcher) {
            // Arrange
            // Flow: unconfirmed user emits verification navigation effect
            viewModel.onEmailChanged("test@example.com")
            viewModel.onPasswordChanged("password1")
            coEvery { signInUseCase(any()) } returns AuthResult.Error(
                AuthError.UserNotConfirmed()
            )

            // Act & Assert
            viewModel.effect.test {
                viewModel.onSignIn()
                advanceUntilIdle()

                Assertions.assertEquals(
                    LoginEffect.NavigateToVerification(Email.of("test@example.com")),
                    awaitItem()
                )
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onSignIn sets passwordError when sign in returns InvalidCredentials error`() = runTest(testDispatcher) {
        // Arrange
        // Error: invalid credentials maps to password field error
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password1")
        coEvery { signInUseCase(any()) } returns AuthResult.Error(
            AuthError.InvalidCredentials("wrong")
        )

        // Act
        viewModel.onSignIn()
        advanceUntilIdle()

        // Assert
        Assertions.assertEquals(
            AuthFieldError.InvalidCredentials,
            viewModel.uiState.value.passwordError
        )
    }

    @Test
    fun `onSignIn emits ShowSnackbar when sign in returns Network error`() = runTest(testDispatcher) {
        // Arrange
        // Flow: network error emits snackbar effect
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password1")
        coEvery { signInUseCase(any()) } returns AuthResult.Error(
            AuthError.Network("offline")
        )

        // Act & Assert
        viewModel.effect.test {
            viewModel.onSignIn()
            advanceUntilIdle()

            Assertions.assertEquals(
                LoginEffect.ShowSnackbar(AuthSnackbarMessage.Network),
                awaitItem()
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onSignUp emits NavigateToVerification when signUp returns Success`() = runTest(testDispatcher) {
        // Arrange
        // Flow: sign-up success emits verification navigation effect
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password1")
        coEvery { signUpUseCase(any()) } returns AuthResult.Success(Unit)

        // Act & Assert
        viewModel.effect.test {
            viewModel.onSignUp()
            advanceUntilIdle()

            Assertions.assertEquals(
                LoginEffect.NavigateToVerification(Email.of("test@example.com")),
                awaitItem()
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onSignUp sets emailError when sign up returns UsernameAlreadyExists error`() = runTest(testDispatcher) {
        // Arrange
        // Error: existing username maps to email field error
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password1")
        coEvery { signUpUseCase(any()) } returns AuthResult.Error(
            AuthError.UsernameAlreadyExists("exists")
        )

        // Act
        viewModel.onSignUp()
        advanceUntilIdle()

        // Assert
        Assertions.assertEquals(
            AuthFieldError.EmailAlreadyRegistered,
            viewModel.uiState.value.emailError
        )
    }

    @Test
    fun `onSignIn sets emailError when both fields are empty`() = runTest(testDispatcher) {
        // Arrange
        // Boundary: empty credentials set both field errors
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("")

        // Act
        viewModel.onSignIn()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        Assertions.assertEquals(
            LoginFormSnapshot(
                emailError = AuthFieldError.InvalidEmail,
                passwordError = AuthFieldError.PasswordTooShort
            ),
            LoginFormSnapshot.from(state)
        )
    }

    @Test
    fun `onSignIn ignores duplicate call when isLoading is true`() = runTest(testDispatcher) {
        // Arrange
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password1")
        coEvery { signInUseCase(any()) } coAnswers {
            delay(1000.milliseconds)
            AuthResult.Success(SignInState.SignedIn)
        }

        // Act
        viewModel.onSignIn()
        viewModel.onSignIn()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { signInUseCase(any()) }
    }

    @Test
    fun `onSignUp ignores duplicate call when isLoading is true`() = runTest(testDispatcher) {
        // Arrange
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password1")
        coEvery { signUpUseCase(any()) } coAnswers {
            delay(1000.milliseconds)
            AuthResult.Success(Unit)
        }

        // Act
        viewModel.onSignUp()
        viewModel.onSignUp()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { signUpUseCase(any()) }
    }

    @Test
    fun `onSignIn sets emailError when email is invalid`() = runTest(testDispatcher) {
        // Arrange
        // Boundary: invalid email sets only email field error
        viewModel.onEmailChanged("invalid")
        viewModel.onPasswordChanged("password1")

        // Act
        viewModel.onSignIn()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        Assertions.assertEquals(
            LoginFormSnapshot(email = "invalid", password = "password1", emailError = AuthFieldError.InvalidEmail),
            LoginFormSnapshot.from(state)
        )
    }

    @Test
    fun `onSignIn sets passwordError when password is too short`() = runTest(testDispatcher) {
        // Arrange
        // Boundary: short password sets only password field error
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("short")

        // Act
        viewModel.onSignIn()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        Assertions.assertEquals(
            LoginFormSnapshot(
                email = "test@example.com",
                password = "short",
                passwordError = AuthFieldError.PasswordTooShort
            ),
            LoginFormSnapshot.from(state)
        )
    }

    private data class LoginFormSnapshot(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val emailError: AuthFieldError? = null,
        val passwordError: AuthFieldError? = null
    ) {
        companion object {
            fun from(state: com.appvoyager.cloudphotos.ui.auth.uistate.LoginUiState) = LoginFormSnapshot(
                email = state.email,
                password = state.password,
                isLoading = state.isLoading,
                emailError = state.emailError,
                passwordError = state.passwordError
            )
        }
    }
}
