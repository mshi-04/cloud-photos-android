package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.usecase.ConfirmResetPasswordUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.ResetPasswordUseCase
import com.appvoyager.cloudphotos.ui.auth.effect.AuthSnackbarMessage
import com.appvoyager.cloudphotos.ui.auth.effect.ResetPasswordEffect
import com.appvoyager.cloudphotos.ui.auth.uistate.AuthFieldError
import io.mockk.coEvery
import io.mockk.coVerify
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
class ResetPasswordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val confirmResetPasswordUseCase = mockk<ConfirmResetPasswordUseCase>()
    private val resetPasswordUseCase = mockk<ResetPasswordUseCase>()

    private lateinit var viewModel: ResetPasswordViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = createViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState returns initial reset password state when viewModel is initialized`() {
        // Arrange
        // State: freshly created viewModel exposes an empty reset password form

        // Act
        val state = viewModel.uiState.value

        // Assert
        assertEquals(InitialStateSnapshot(), InitialStateSnapshot.from(state))
    }

    @Test
    fun `onCodeChanged sets pasted digits when value contains multiple digits`() {
        // Arrange
        // State: code fields start empty before pasted input

        // Act
        // Boundary: pasted digits fill available code slots from the target index
        viewModel.onCodeChanged(1, "234567")

        // Assert
        assertEquals(listOf("", "2", "3", "4", "5", "6"), viewModel.uiState.value.codes)
    }

    @Test
    fun `onConfirm emits NavigateBackToLogin when confirmation succeeds`() = runTest(testDispatcher) {
        // Arrange
        fillValidForm()
        coEvery { confirmResetPasswordUseCase(any()) } returns AuthResult.Success(Unit)

        // Act & Assert
        // Flow: successful confirmation emits login navigation effect
        viewModel.effect.test {
            viewModel.onConfirm()
            advanceUntilIdle()

            assertEquals(
                ResetPasswordEffect.NavigateBackToLogin(AuthSnackbarMessage.PasswordReset),
                awaitItem()
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onConfirm sets codeError when confirmation returns CodeMismatch`() = runTest(testDispatcher) {
        // Arrange
        fillValidForm()
        coEvery { confirmResetPasswordUseCase(any()) } returns AuthResult.Error(AuthError.CodeMismatch())

        // Act
        // Error: code mismatch maps to code field error
        viewModel.onConfirm()
        advanceUntilIdle()

        // Assert
        assertEquals(AuthFieldError.CodeMismatch, viewModel.uiState.value.codeError)
    }

    @Test
    fun `onConfirm sets passwordError when confirmation returns InvalidPassword`() = runTest(testDispatcher) {
        // Arrange
        fillValidForm()
        coEvery { confirmResetPasswordUseCase(any()) } returns AuthResult.Error(AuthError.InvalidPassword())

        // Act
        // Error: invalid password maps to password field error
        viewModel.onConfirm()
        advanceUntilIdle()

        // Assert
        assertEquals(AuthFieldError.InvalidPassword, viewModel.uiState.value.passwordError)
    }

    @Test
    fun `onConfirm ignores useCase when form is incomplete`() = runTest(testDispatcher) {
        // Arrange
        viewModel.onCodeChanged(0, "1")
        viewModel.onNewPasswordChanged("password1")

        // Act
        // Interaction: invalid form prevents confirmation dependency call
        viewModel.onConfirm()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { confirmResetPasswordUseCase(any()) }
    }

    @Test
    fun `onResend emits ShowSnackbar when reset password succeeds`() = runTest(testDispatcher) {
        // Arrange
        coEvery { resetPasswordUseCase(any()) } returns AuthResult.Success(Unit)
        elapseResendTimer()

        // Act & Assert
        // Coroutine/Flow: elapsed resend timer allows resend and emits snackbar
        viewModel.effect.test {
            viewModel.onResend()
            advanceUntilIdle()

            assertEquals(
                ResetPasswordEffect.ShowSnackbar(AuthSnackbarMessage.CodeResent),
                awaitItem()
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onResend ignores useCase when resend timer is active`() = runTest(testDispatcher) {
        // Arrange
        // Coroutine/State: resend timer is active immediately after initialization

        // Act
        // Interaction: active resend timer prevents reset password dependency call
        viewModel.onResend()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { resetPasswordUseCase(any()) }
    }

    private fun createViewModel(email: String = "test@example.com"): ResetPasswordViewModel = ResetPasswordViewModel(
        savedStateHandle = SavedStateHandle(mapOf("email" to email)),
        confirmResetPasswordUseCase = confirmResetPasswordUseCase,
        resetPasswordUseCase = resetPasswordUseCase
    )

    private fun fillValidForm() {
        viewModel.onCodeChanged(0, "123456")
        viewModel.onNewPasswordChanged("password1")
    }

    private fun elapseResendTimer() {
        viewModel.startTimerIfNeeded()
        repeat(61) {
            testDispatcher.scheduler.advanceTimeBy(1_000L)
            testDispatcher.scheduler.runCurrent()
        }
    }

    private data class InitialStateSnapshot(
        val codes: List<String> = List(6) { "" },
        val newPassword: String = "",
        val isNewPasswordVisible: Boolean = false,
        val isLoading: Boolean = false,
        val codeError: AuthFieldError? = null,
        val passwordError: AuthFieldError? = null,
        val resendTimerSeconds: Int = 60
    ) {
        companion object {
            fun from(state: com.appvoyager.cloudphotos.ui.auth.uistate.ResetPasswordUiState) = InitialStateSnapshot(
                codes = state.codes,
                newPassword = state.newPassword,
                isNewPasswordVisible = state.isNewPasswordVisible,
                isLoading = state.isLoading,
                codeError = state.codeError,
                passwordError = state.passwordError,
                resendTimerSeconds = state.resendTimerSeconds
            )
        }
    }
}
