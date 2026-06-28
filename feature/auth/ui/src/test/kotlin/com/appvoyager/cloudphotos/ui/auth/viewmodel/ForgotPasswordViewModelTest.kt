package com.appvoyager.cloudphotos.ui.auth.viewmodel

import app.cash.turbine.test
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.usecase.ResetPasswordUseCase
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.ui.auth.effect.AuthSnackbarMessage
import com.appvoyager.cloudphotos.ui.auth.effect.ForgotPasswordEffect
import com.appvoyager.cloudphotos.ui.auth.uistate.AuthFieldError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val resetPasswordUseCase = mockk<ResetPasswordUseCase>()

    private lateinit var viewModel: ForgotPasswordViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ForgotPasswordViewModel(resetPasswordUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState sets email to empty when viewModel is initialized`() {
        // Arrange
        // State: freshly created viewModel exposes empty initial form state

        // Act
        val state = viewModel.uiState.value

        // Assert
        assertEquals(ForgotPasswordFormSnapshot(), ForgotPasswordFormSnapshot.from(state))
    }

    @Test
    fun `onEmailChanged sets email when called`() {
        // Arrange
        // State: email field starts empty before user input

        // Act
        viewModel.onEmailChanged("test@example.com")
        val state = viewModel.uiState.value

        // Assert
        assertEquals(
            ForgotPasswordFormSnapshot(email = "test@example.com"),
            ForgotPasswordFormSnapshot.from(state)
        )
    }

    @Test
    fun `onClearEmail sets email to empty when called`() {
        // Arrange
        viewModel.onEmailChanged("test@example.com")

        // Act
        // State: clear action resets only the email text
        viewModel.onClearEmail()

        // Assert
        assertEquals("", viewModel.uiState.value.email)
    }

    @Test
    fun `onSubmit sets emailError when email is invalid`() = runTest(testDispatcher) {
        // Arrange
        viewModel.onEmailChanged("invalid-email")

        // Act
        // Boundary: invalid email prevents submit and sets field error
        viewModel.onSubmit()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals(
            ForgotPasswordFormSnapshot(email = "invalid-email", emailError = AuthFieldError.InvalidEmail),
            ForgotPasswordFormSnapshot.from(state)
        )
    }

    @Test
    fun `onSubmit emits NavigateToResetPassword when reset password succeeds`() = runTest(testDispatcher) {
        // Arrange
        viewModel.onEmailChanged("test@example.com")
        coEvery { resetPasswordUseCase(any()) } returns AuthResult.Success(Unit)

        // Act & Assert
        // Flow: reset password success emits reset navigation effect
        viewModel.effect.test {
            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(
                ForgotPasswordEffect.NavigateToResetPassword(Email.of("test@example.com")),
                awaitItem()
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onSubmit emits ShowSnackbar when resetPasswordUseCase returns Network error`() = runTest(testDispatcher) {
        // Arrange
        viewModel.onEmailChanged("test@example.com")
        coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
            AuthError.Network()
        )

        // Act & Assert
        // Flow: network error emits snackbar effect
        viewModel.effect.test {
            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(
                ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.Network),
                awaitItem()
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onSubmit emits ShowSnackbar when resetPasswordUseCase returns TooManyRequests error`() =
        runTest(testDispatcher) {
            // Arrange
            viewModel.onEmailChanged("test@example.com")
            coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
                AuthError.TooManyRequests()
            )

            // Act & Assert
            // Flow: too many requests error emits snackbar effect
            viewModel.effect.test {
                viewModel.onSubmit()
                advanceUntilIdle()

                assertEquals(
                    ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.TooManyRequests),
                    awaitItem()
                )
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onSubmit emits NavigateToVerification when resetPasswordUseCase returns UserNotConfirmed error`() =
        runTest(testDispatcher) {
            // Arrange
            viewModel.onEmailChanged("test@example.com")
            coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
                AuthError.UserNotConfirmed()
            )

            // Act & Assert
            // Flow: unconfirmed user emits verification navigation effect
            viewModel.effect.test {
                viewModel.onSubmit()
                advanceUntilIdle()

                assertEquals(
                    ForgotPasswordEffect.NavigateToVerification(Email.of("test@example.com")),
                    awaitItem()
                )
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onSubmit emits ShowSnackbar when resetPasswordUseCase returns InvalidCredentials error`() =
        runTest(testDispatcher) {
            // Arrange
            viewModel.onEmailChanged("test@example.com")
            coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
                AuthError.InvalidCredentials()
            )

            // Act & Assert
            // Flow: invalid credentials error emits snackbar effect
            viewModel.effect.test {
                viewModel.onSubmit()
                advanceUntilIdle()

                assertEquals(
                    ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.InvalidCredentials),
                    awaitItem()
                )
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onSubmit emits ShowSnackbar when resetPasswordUseCase returns InvalidPassword error`() =
        runTest(testDispatcher) {
            // Arrange
            viewModel.onEmailChanged("test@example.com")
            coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
                AuthError.InvalidPassword()
            )

            // Act & Assert
            // Flow: invalid password error emits snackbar effect
            viewModel.effect.test {
                viewModel.onSubmit()
                advanceUntilIdle()

                assertEquals(
                    ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.InvalidPassword),
                    awaitItem()
                )
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `onSubmit emits ShowSnackbar when resetPasswordUseCase returns CodeExpired error`() = runTest(testDispatcher) {
        // Arrange
        viewModel.onEmailChanged("test@example.com")
        coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
            AuthError.CodeExpired()
        )

        // Act & Assert
        // Flow: expired code error emits snackbar effect
        viewModel.effect.test {
            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(
                ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.CodeExpired),
                awaitItem()
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onSubmit emits ShowSnackbar when resetPasswordUseCase returns CodeMismatch error`() = runTest(testDispatcher) {
        // Arrange
        viewModel.onEmailChanged("test@example.com")
        coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
            AuthError.CodeMismatch()
        )

        // Act & Assert
        // Flow: mismatched code error emits snackbar effect
        viewModel.effect.test {
            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(
                ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.CodeMismatch),
                awaitItem()
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onSubmit emits ShowSnackbar when resetPasswordUseCase returns Unknown error`() = runTest(testDispatcher) {
        // Arrange
        viewModel.onEmailChanged("test@example.com")
        coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
            AuthError.Unknown()
        )

        // Act & Assert
        // Flow: unknown error emits snackbar effect
        viewModel.effect.test {
            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(
                ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.Unknown),
                awaitItem()
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onSubmit emits ShowSnackbar when resetPasswordUseCase returns UsernameAlreadyExists error`() =
        runTest(testDispatcher) {
            // Arrange
            viewModel.onEmailChanged("test@example.com")
            coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
                AuthError.UsernameAlreadyExists()
            )

            // Act & Assert
            // Flow: unexpected username error emits unknown snackbar effect
            viewModel.effect.test {
                viewModel.onSubmit()
                advanceUntilIdle()

                assertEquals(
                    ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.Unknown),
                    awaitItem()
                )
                cancelAndConsumeRemainingEvents()
            }
        }

    private data class ForgotPasswordFormSnapshot(
        val email: String = "",
        val isLoading: Boolean = false,
        val emailError: AuthFieldError? = null
    ) {
        companion object {
            fun from(state: com.appvoyager.cloudphotos.ui.auth.uistate.ForgotPasswordUiState) =
                ForgotPasswordFormSnapshot(
                    email = state.email,
                    isLoading = state.isLoading,
                    emailError = state.emailError
                )
        }
    }
}
