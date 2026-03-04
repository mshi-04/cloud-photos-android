package com.appvoyager.cloudphotos.ui.auth.viewmodel

import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.usecase.SignInUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.SignUpUseCase
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.ui.auth.effect.LoginEffect
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
        viewModel = LoginViewModel(signInUseCase, signUpUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty email and password`() {
        val state = viewModel.uiState.value
        Assertions.assertEquals("", state.email)
        Assertions.assertEquals("", state.password)
        Assertions.assertFalse(state.isLoading)
        Assertions.assertNull(state.emailError)
        Assertions.assertNull(state.passwordError)
    }

    @Test
    fun `onEmailChanged updates email and clears error`() {
        viewModel.onEmailChanged("test@example.com")
        val state = viewModel.uiState.value
        Assertions.assertEquals("test@example.com", state.email)
        Assertions.assertNull(state.emailError)
    }

    @Test
    fun `onPasswordChanged updates password and clears error`() {
        viewModel.onPasswordChanged("password1")
        val state = viewModel.uiState.value
        Assertions.assertEquals("password1", state.password)
        Assertions.assertNull(state.passwordError)
    }

    @Test
    fun `isFormValid returns true for valid input`() {
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password1")
        Assertions.assertTrue(viewModel.isFormValid)
    }

    @Test
    fun `isFormValid returns false for invalid email`() {
        viewModel.onEmailChanged("invalid")
        viewModel.onPasswordChanged("password1")
        Assertions.assertFalse(viewModel.isFormValid)
    }

    @Test
    fun `isFormValid returns false for short password`() {
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("short")
        Assertions.assertFalse(viewModel.isFormValid)
    }

    @Test
    fun `onSignIn success emits NavigateToHome`() = runTest(testDispatcher) {
        // Arrange
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password1")
        coEvery { signInUseCase(any()) } returns AuthResult.Success(SignInState.SignedIn)

        // Act
        var effect: LoginEffect? = null
        val job = launch { effect = viewModel.effect.first() }
        viewModel.onSignIn()
        advanceUntilIdle()

        // Assert
        Assertions.assertTrue(effect is LoginEffect.NavigateToHome)
        Assertions.assertFalse(viewModel.uiState.value.isLoading)
        job.cancel()
    }

    @Test
    fun `onSignIn with UserNotConfirmed emits NavigateToVerification`() =
        runTest(testDispatcher) {
            // Arrange
            viewModel.onEmailChanged("test@example.com")
            viewModel.onPasswordChanged("password1")
            coEvery { signInUseCase(any()) } returns AuthResult.Error(
                AuthError.UserNotConfirmed()
            )

            // Act
            var effect: LoginEffect? = null
            val job = launch { effect = viewModel.effect.first() }
            viewModel.onSignIn()
            advanceUntilIdle()

            // Assert
            Assertions.assertTrue(effect is LoginEffect.NavigateToVerification)
            Assertions.assertEquals(
                Email.of("test@example.com"),
                (effect as LoginEffect.NavigateToVerification).email
            )
            job.cancel()
        }

    @Test
    fun `onSignIn with InvalidCredentials sets passwordError`() = runTest(testDispatcher) {
        // Arrange
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
            R.string.error_invalid_credentials,
            viewModel.uiState.value.passwordError
        )
    }

    @Test
    fun `onSignIn with Network error emits ShowSnackbar`() = runTest(testDispatcher) {
        // Arrange
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password1")
        coEvery { signInUseCase(any()) } returns AuthResult.Error(
            AuthError.Network("offline")
        )

        // Act
        var effect: LoginEffect? = null
        val job = launch { effect = viewModel.effect.first() }
        viewModel.onSignIn()
        advanceUntilIdle()

        // Assert
        Assertions.assertTrue(effect is LoginEffect.ShowSnackbar)
        job.cancel()
    }

    @Test
    fun `onSignUp success emits NavigateToVerification`() = runTest(testDispatcher) {
        // Arrange
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password1")
        coEvery { signUpUseCase(any()) } returns AuthResult.Success(Unit)

        // Act
        var effect: LoginEffect? = null
        val job = launch { effect = viewModel.effect.first() }
        viewModel.onSignUp()
        advanceUntilIdle()

        // Assert
        Assertions.assertTrue(effect is LoginEffect.NavigateToVerification)
        Assertions.assertEquals(
            Email.of("test@example.com"),
            (effect as LoginEffect.NavigateToVerification).email
        )
        job.cancel()
    }

    @Test
    fun `onSignUp with UsernameAlreadyExists sets emailError`() = runTest(testDispatcher) {
        // Arrange
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
            R.string.error_email_already_registered,
            viewModel.uiState.value.emailError
        )
    }

    @Test
    fun `onSignIn with invalid form sets error messages`() = runTest(testDispatcher) {
        // Arrange
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("")

        // Act
        viewModel.onSignIn()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        Assertions.assertEquals(R.string.error_invalid_email, state.emailError)
        Assertions.assertEquals(R.string.error_password_too_short, state.passwordError)
    }
}