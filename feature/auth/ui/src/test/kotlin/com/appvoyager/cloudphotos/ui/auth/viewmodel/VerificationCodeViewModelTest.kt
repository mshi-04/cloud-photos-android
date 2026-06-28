package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.usecase.ConfirmSignUpUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.ResendSignUpCodeUseCase
import com.appvoyager.cloudphotos.ui.auth.effect.AuthSnackbarMessage
import com.appvoyager.cloudphotos.ui.auth.effect.VerificationEffect
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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VerificationCodeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val confirmSignUpUseCase = mockk<ConfirmSignUpUseCase>()
    private val resendSignUpCodeUseCase = mockk<ResendSignUpCodeUseCase>()

    private lateinit var viewModel: VerificationCodeViewModel

    private fun createViewModel(email: String = "test@example.com"): VerificationCodeViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf("email" to email)
        )
        return VerificationCodeViewModel(
            savedStateHandle = savedStateHandle,
            confirmSignUpUseCase = confirmSignUpUseCase,
            resendSignUpCodeUseCase = resendSignUpCodeUseCase
        )
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = createViewModel()
        viewModel.startTimer()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState sets codes to empty when viewModel is initialized`() {
        // Arrange
        // State: freshly created viewModel exposes empty verification code fields

        // Act
        val state = viewModel.uiState.value

        // Assert
        Assertions.assertEquals(VerificationCodeSnapshot(), VerificationCodeSnapshot.from(state))
    }

    @Test
    fun `onCodeChanged sets single digit when single character is entered`() {
        // Arrange
        // State: code fields start empty before single digit input

        // Act
        // State: single digit input updates only the targeted field
        viewModel.onCodeChanged(0, "1")
        val state = viewModel.uiState.value

        // Assert
        Assertions.assertEquals(
            VerificationCodeSnapshot(codes = listOf("1", "", "", "", "", "")),
            VerificationCodeSnapshot.from(state)
        )
    }

    @Test
    fun `onCodeChanged sets all digits when paste input of 6 characters is entered`() {
        // Arrange
        // State: code fields start empty before paste input

        // Act
        // Boundary: six pasted digits fill all verification fields
        viewModel.onCodeChanged(0, "123456")

        // Assert
        Assertions.assertEquals(
            listOf("1", "2", "3", "4", "5", "6"),
            viewModel.uiState.value.codes
        )
    }

    @Test
    fun `isCodeComplete returns true when all 6 digits are filled`() {
        // Arrange
        repeat(6) { i -> viewModel.onCodeChanged(i, (i + 1).toString()) }

        // Act
        // Boundary: exactly six filled digits complete the code
        val isCodeComplete = viewModel.uiState.value.isCodeComplete

        // Assert
        Assertions.assertTrue(isCodeComplete)
    }

    @Test
    fun `onVerify emits NavigateToHome when confirmation succeeds`() = runTest(testDispatcher) {
        // Arrange
        // Flow: confirmation success emits home navigation effect
        coEvery { confirmSignUpUseCase(any()) } returns AuthResult.Success(Unit)

        fillCode("12345")

        // Act & Assert
        viewModel.effect.test {
            viewModel.onCodeChanged(5, "6")
            advanceUntilIdle()

            Assertions.assertEquals(VerificationEffect.NavigateToHome, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onVerify sets codeError when confirmation returns CodeMismatch`() = runTest(testDispatcher) {
        // Arrange
        // Error: code mismatch maps to code field error
        coEvery { confirmSignUpUseCase(any()) } returns AuthResult.Error(
            AuthError.CodeMismatch("wrong code")
        )

        fillCode("12345")

        // Act
        viewModel.onCodeChanged(5, "6")
        advanceUntilIdle()

        // Assert
        Assertions.assertEquals(
            AuthFieldError.CodeMismatch,
            viewModel.uiState.value.codeError
        )
    }

    @Test
    fun `onVerify sets codeError when confirmation returns CodeExpired`() = runTest(testDispatcher) {
        // Arrange
        // Error: expired code maps to code field error
        coEvery { confirmSignUpUseCase(any()) } returns AuthResult.Error(
            AuthError.CodeExpired("expired")
        )

        fillCode("12345")

        // Act
        viewModel.onCodeChanged(5, "6")
        advanceUntilIdle()

        // Assert
        Assertions.assertEquals(
            AuthFieldError.CodeExpired,
            viewModel.uiState.value.codeError
        )
    }

    @Test
    fun `onVerify emits ShowSnackbar when confirmation returns Network error`() = runTest(testDispatcher) {
        // Arrange
        // Flow: network error emits snackbar effect
        coEvery { confirmSignUpUseCase(any()) } returns AuthResult.Error(
            AuthError.Network("offline")
        )

        fillCode("12345")

        // Act & Assert
        viewModel.effect.test {
            viewModel.onCodeChanged(5, "6")
            advanceUntilIdle()

            Assertions.assertEquals(
                VerificationEffect.ShowSnackbar(AuthSnackbarMessage.Network),
                awaitItem()
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onResend emits ShowSnackbar when resend succeeds`() = runTest(testDispatcher) {
        // Arrange
        // Coroutine/Flow: elapsed resend timer allows resend and emits snackbar
        coEvery { resendSignUpCodeUseCase(any()) } returns AuthResult.Success(Unit)

        repeat(61) {
            testDispatcher.scheduler.advanceTimeBy(1_000L)
            testDispatcher.scheduler.runCurrent()
        }

        // Act & Assert
        viewModel.effect.test {
            viewModel.onResend()

            testDispatcher.scheduler.advanceTimeBy(100L)
            testDispatcher.scheduler.runCurrent()

            Assertions.assertEquals(
                VerificationEffect.ShowSnackbar(AuthSnackbarMessage.CodeResent),
                awaitItem()
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    private fun fillCode(code: String) {
        code.take(6).forEachIndexed { i, ch ->
            viewModel.onCodeChanged(i, ch.toString())
        }
    }

    private data class VerificationCodeSnapshot(
        val codes: List<String> = List(6) { "" },
        val isLoading: Boolean = false,
        val codeError: AuthFieldError? = null,
        val resendTimerSeconds: Int = 60,
        val isResendEnabled: Boolean = false
    ) {
        companion object {
            fun from(state: com.appvoyager.cloudphotos.ui.auth.uistate.VerificationCodeUiState) =
                VerificationCodeSnapshot(
                    codes = state.codes,
                    isLoading = state.isLoading,
                    codeError = state.codeError,
                    resendTimerSeconds = state.resendTimerSeconds,
                    isResendEnabled = state.isResendEnabled
                )
        }
    }
}
