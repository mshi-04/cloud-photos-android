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
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@OptIn(ExperimentalCoroutinesApi::class)
class ResetPasswordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val confirmResetPasswordUseCase = mockk<ConfirmResetPasswordUseCase>()
    private val resetPasswordUseCase = mockk<ResetPasswordUseCase>()

    private lateinit var viewModel: ResetPasswordViewModel

    @BeforeEach
    fun setUp() {
        clearAllMocks()
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
        assertFalse(viewModel.isCodeComplete)
        assertFalse(viewModel.isFormValid)
        assertFalse(viewModel.isResendEnabled)
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
        assertFalse(viewModel.isCodeComplete)
    }

    @Test
    fun `onConfirm emits NavigateBackToLogin when confirmation succeeds`() = runTest(testDispatcher) {
        // Arrange
        fillValidForm()
        assertTrue(viewModel.isFormValid)
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
            runCurrent()
            expectNoEvents()
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
        assertFalse(viewModel.isFormValid)
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

    @ParameterizedTest
    @CsvSource(
        "1234567, false",
        "12345678, true",
        "' 1234567 ', false",
        "' 12345678 ', true",
        "'        ', false"
    )
    fun `isFormValid returns expected result when password length changes`(password: String, expected: Boolean) {
        // Arrange
        viewModel.onCodeChanged(0, "123456")

        // Act
        // Boundary: validity uses trimmed password length at the seven/eight character boundary
        viewModel.onNewPasswordChanged(password)
        val actual = viewModel.isFormValid

        // Assert
        assertTrue(viewModel.isCodeComplete)
        assertEquals(expected, actual)
        assertEquals(password, viewModel.uiState.value.newPassword)
    }

    @Test
    fun `isCodeComplete returns false when a completed code is cleared`() {
        // Arrange
        fillValidForm()
        assertTrue(viewModel.isCodeComplete)
        assertTrue(viewModel.isFormValid)

        // Act
        // State: deleting a digit immediately invalidates both code and form without stored booleans
        viewModel.onCodeChanged(2, "")

        // Assert
        assertFalse(viewModel.isCodeComplete)
        assertFalse(viewModel.isFormValid)
    }

    @Test
    fun `isFormValid returns captured validity when current code loses a digit`() {
        // Arrange
        fillValidForm()
        val displayedState = viewModel.uiState.value

        // Act
        // State: the displayed code and confirm flag stay paired while a newer code is incomplete
        viewModel.onCodeChanged(2, "")
        val displayedValidity = viewModel.isFormValid(displayedState)

        // Assert
        assertTrue(displayedValidity)
        assertFalse(viewModel.isFormValid)
        assertEquals(listOf("1", "2", "3", "4", "5", "6"), displayedState.codes)
        assertEquals("", viewModel.uiState.value.codes[2])
    }

    @Test
    fun `isResendEnabled returns captured availability when current cooldown expires`() = runTest(testDispatcher) {
        // Arrange
        viewModel.startTimerIfNeeded()
        runCurrent()
        testScheduler.advanceTimeBy(59_000L)
        runCurrent()
        val displayedState = viewModel.uiState.value

        // Act
        // Coroutine/State: the displayed countdown keeps resend disabled until its own snapshot reaches zero
        testScheduler.advanceTimeBy(1_000L)
        runCurrent()
        val displayedAvailability = viewModel.isResendEnabled(displayedState)

        // Assert
        assertFalse(displayedAvailability)
        assertTrue(viewModel.isResendEnabled)
        assertEquals(1, displayedState.resendTimerSeconds)
        assertEquals(0, viewModel.uiState.value.resendTimerSeconds)
    }

    @Test
    fun `isResendEnabled returns true when cooldown reaches zero`() = runTest(testDispatcher) {
        // Arrange
        viewModel.startTimerIfNeeded()
        runCurrent()
        testScheduler.advanceTimeBy(59_000L)
        runCurrent()
        assertEquals(1, viewModel.uiState.value.resendTimerSeconds)
        assertFalse(viewModel.isResendEnabled)

        // Act
        // Coroutine/Boundary: resend becomes enabled exactly when the final second expires
        testScheduler.advanceTimeBy(1_000L)
        runCurrent()

        // Assert
        assertEquals(0, viewModel.uiState.value.resendTimerSeconds)
        assertTrue(viewModel.isResendEnabled)
    }

    @Test
    fun `onResend ignores duplicate request when resend is loading`() = runTest(testDispatcher) {
        // Arrange
        val response = CompletableDeferred<AuthResult<Unit>>()
        coEvery { resetPasswordUseCase(any()) } coAnswers { response.await() }
        elapseResendTimer()
        assertTrue(viewModel.isResendEnabled)

        // Act
        // Coroutine/Interaction: loading disables resend before a successful response restarts the cooldown
        viewModel.onResend()
        viewModel.onResend()
        runCurrent()
        val enabledWhileLoading = viewModel.isResendEnabled
        val loadingDuringRequest = viewModel.uiState.value.isLoading
        response.complete(AuthResult.Success(Unit))
        runCurrent()

        // Assert
        assertFalse(enabledWhileLoading)
        assertTrue(loadingDuringRequest)
        coVerify(exactly = 1) { resetPasswordUseCase(any()) }
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(60, viewModel.uiState.value.resendTimerSeconds)
        assertFalse(viewModel.isResendEnabled)
    }

    @Test
    fun `onConfirm emits queued navigation when collection starts after completion`() = runTest(testDispatcher) {
        // Arrange
        fillValidForm()
        coEvery { confirmResetPasswordUseCase(any()) } returns AuthResult.Success(Unit)

        // Act
        // Flow: password-reset completion survives until a Screen collector subscribes
        viewModel.onConfirm()
        runCurrent()

        // Assert
        viewModel.effect.test {
            assertEquals(ResetPasswordEffect.NavigateBackToLogin(AuthSnackbarMessage.PasswordReset), awaitItem())
            runCurrent()
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 1) { confirmResetPasswordUseCase(any()) }
    }

    @Test
    fun `onConfirm emits only queued effects when collection restarts`() = runTest(testDispatcher) {
        // Arrange
        fillValidForm()
        coEvery { confirmResetPasswordUseCase(any()) } returnsMany listOf(
            AuthResult.Error(AuthError.Network()),
            AuthResult.Error(AuthError.TooManyRequests())
        )
        viewModel.effect.test {
            viewModel.onConfirm()
            runCurrent()
            assertEquals(ResetPasswordEffect.ShowSnackbar(AuthSnackbarMessage.Network), awaitItem())
            cancelAndConsumeRemainingEvents()
        }

        // Act
        // Flow: restarting collection receives only the result produced while no collector was present
        viewModel.onConfirm()
        runCurrent()

        // Assert
        viewModel.effect.test {
            assertEquals(ResetPasswordEffect.ShowSnackbar(AuthSnackbarMessage.TooManyRequests), awaitItem())
            runCurrent()
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
        viewModel.effect.test {
            runCurrent()
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
        coVerify(exactly = 2) { confirmResetPasswordUseCase(any()) }
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
