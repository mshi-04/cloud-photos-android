package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.lifecycle.SavedStateHandle
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
        val state = viewModel.uiState.value
        Assertions.assertEquals(List(6) { "" }, state.codes)
        Assertions.assertFalse(state.isLoading)
        Assertions.assertNull(state.codeError)
        Assertions.assertEquals(60, state.resendTimerSeconds)
        Assertions.assertFalse(state.isResendEnabled)
    }

    @Test
    fun `onCodeChanged sets single digit when single character is entered`() {
        viewModel.onCodeChanged(0, "1")
        val state = viewModel.uiState.value
        Assertions.assertEquals("1", state.codes[0])
        Assertions.assertEquals("", state.codes[1])
    }

    @Test
    fun `onCodeChanged sets all digits when paste input of 6 characters is entered`() {
        viewModel.onCodeChanged(0, "123456")
        Assertions.assertEquals(
            listOf("1", "2", "3", "4", "5", "6"),
            viewModel.uiState.value.codes
        )
    }

    @Test
    fun `isCodeComplete returns true when all 6 digits are filled`() {
        repeat(6) { i -> viewModel.onCodeChanged(i, (i + 1).toString()) }
        Assertions.assertTrue(viewModel.uiState.value.isCodeComplete)
    }

    @Test
    fun `onVerify emits NavigateToHome when confirmation succeeds`() = runTest(testDispatcher) {
        // Arrange
        coEvery { confirmSignUpUseCase(any()) } returns AuthResult.Success(Unit)

        fillCode("12345")

        // Act
        var effect: VerificationEffect? = null
        val job = launch { effect = viewModel.effect.first() }
        viewModel.onCodeChanged(5, "6")
        advanceUntilIdle()

        // Assert
        Assertions.assertTrue(effect is VerificationEffect.NavigateToHome)
        job.cancel()
    }

    @Test
    fun `onVerify sets codeError when confirmation returns CodeMismatch`() = runTest(testDispatcher) {
        // Arrange
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
        coEvery { confirmSignUpUseCase(any()) } returns AuthResult.Error(
            AuthError.Network("offline")
        )

        fillCode("12345")

        // Act
        var effect: VerificationEffect? = null
        val job = launch { effect = viewModel.effect.first() }
        viewModel.onCodeChanged(5, "6")
        advanceUntilIdle()

        // Assert
        Assertions.assertTrue(effect is VerificationEffect.ShowSnackbar)
        job.cancel()
    }

    @Test
    fun `onResend emits ShowSnackbar when resend succeeds`() = runTest(testDispatcher) {
        // Arrange
        coEvery { resendSignUpCodeUseCase(any()) } returns AuthResult.Success(Unit)

        repeat(61) {
            testDispatcher.scheduler.advanceTimeBy(1_000L)
            testDispatcher.scheduler.runCurrent()
        }

        Assertions.assertTrue(
            viewModel.uiState.value.resendTimerSeconds <= 0,
            "Timer should have elapsed, but was ${viewModel.uiState.value.resendTimerSeconds}"
        )

        // Act
        var effect: VerificationEffect? = null
        val job = launch { effect = viewModel.effect.first() }

        viewModel.onResend()

        testDispatcher.scheduler.advanceTimeBy(100L)
        testDispatcher.scheduler.runCurrent()

        // Assert
        Assertions.assertTrue(effect is VerificationEffect.ShowSnackbar)
        Assertions.assertEquals(
            AuthSnackbarMessage.CodeResent,
            (effect as VerificationEffect.ShowSnackbar).message
        )
        Assertions.assertTrue(
            viewModel.uiState.value.resendTimerSeconds > 0,
            "Timer should have been reset"
        )
        job.cancel()
    }

    private fun fillCode(code: String) {
        code.take(6).forEachIndexed { i, ch ->
            viewModel.onCodeChanged(i, ch.toString())
        }
    }
}
