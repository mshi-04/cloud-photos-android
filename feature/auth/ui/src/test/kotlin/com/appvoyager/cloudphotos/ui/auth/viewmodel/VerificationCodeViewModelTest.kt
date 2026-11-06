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
        clearAllMocks()
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
        Assertions.assertFalse(viewModel.isCodeComplete)
        Assertions.assertFalse(viewModel.isResendEnabled)
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
        val isCodeComplete = viewModel.isCodeComplete

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
            runCurrent()
            expectNoEvents()
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

    @Test
    fun `isCodeComplete returns false when a verified code loses a digit`() = runTest(testDispatcher) {
        // Arrange
        coEvery { confirmSignUpUseCase(any()) } returns AuthResult.Error(AuthError.CodeMismatch())
        fillCode("123456")
        runCurrent()
        Assertions.assertTrue(viewModel.isCodeComplete)

        // Act
        // State: clearing a digit updates validity and prevents another confirmation request
        viewModel.onCodeChanged(2, "")
        viewModel.onVerify()
        runCurrent()

        // Assert
        Assertions.assertFalse(viewModel.isCodeComplete)
        coVerify(exactly = 1) { confirmSignUpUseCase(any()) }
    }

    @Test
    fun `isCodeComplete returns captured validity when current code loses a digit`() = runTest(testDispatcher) {
        // Arrange
        coEvery { confirmSignUpUseCase(any()) } returns AuthResult.Error(AuthError.CodeMismatch())
        fillCode("123456")
        runCurrent()
        val displayedState = viewModel.uiState.value

        // Act
        // State: the displayed verification code and enabled flag keep the same completed snapshot
        viewModel.onCodeChanged(2, "")
        val displayedValidity = viewModel.isCodeComplete(displayedState)

        // Assert
        Assertions.assertTrue(displayedValidity)
        Assertions.assertFalse(viewModel.isCodeComplete)
        Assertions.assertEquals(listOf("1", "2", "3", "4", "5", "6"), displayedState.codes)
        Assertions.assertEquals("", viewModel.uiState.value.codes[2])
    }

    @Test
    fun `onCodeChanged calls confirmation once when pasted input exceeds six digits`() = runTest(testDispatcher) {
        // Arrange
        coEvery { confirmSignUpUseCase(any()) } returns AuthResult.Error(AuthError.CodeMismatch())

        // Act
        // Boundary/Interaction: filtered paste completes six slots and triggers automatic confirmation once
        viewModel.onCodeChanged(0, "1a2b3c4d5e6f7")
        runCurrent()

        // Assert
        Assertions.assertEquals(listOf("1", "2", "3", "4", "5", "6"), viewModel.uiState.value.codes)
        Assertions.assertTrue(viewModel.isCodeComplete)
        coVerify(exactly = 1) { confirmSignUpUseCase(match { it.code.value == "123456" }) }
        coVerify(exactly = 1) { confirmSignUpUseCase(any()) }
    }

    @Test
    fun `isResendEnabled returns captured availability when current resend starts loading`() = runTest(testDispatcher) {
        // Arrange
        val response = CompletableDeferred<AuthResult<Unit>>()
        coEvery { resendSignUpCodeUseCase(any()) } coAnswers { response.await() }
        runCurrent()
        testScheduler.advanceTimeBy(60_000L)
        runCurrent()
        val displayedState = viewModel.uiState.value

        // Act
        // Coroutine/State: a newer request does not change the resend flag for the displayed idle snapshot
        viewModel.onResend()
        runCurrent()
        val displayedAvailability = viewModel.isResendEnabled(displayedState)
        val currentAvailability = viewModel.isResendEnabled
        val currentLoading = viewModel.uiState.value.isLoading
        response.complete(AuthResult.Error(AuthError.Network()))
        runCurrent()

        // Assert
        Assertions.assertTrue(displayedAvailability)
        Assertions.assertFalse(currentAvailability)
        Assertions.assertFalse(displayedState.isLoading)
        Assertions.assertTrue(currentLoading)
        coVerify(exactly = 1) { resendSignUpCodeUseCase(any()) }
    }

    @Test
    fun `isResendEnabled returns true when cooldown reaches zero`() = runTest(testDispatcher) {
        // Arrange
        runCurrent()
        testScheduler.advanceTimeBy(59_000L)
        runCurrent()
        Assertions.assertEquals(1, viewModel.uiState.value.resendTimerSeconds)
        Assertions.assertFalse(viewModel.isResendEnabled)

        // Act
        // Coroutine/Boundary: the derived resend flag follows the last timer tick
        testScheduler.advanceTimeBy(1_000L)
        runCurrent()

        // Assert
        Assertions.assertEquals(0, viewModel.uiState.value.resendTimerSeconds)
        Assertions.assertTrue(viewModel.isResendEnabled)
    }

    @Test
    fun `onResend ignores duplicate request when resend is loading`() = runTest(testDispatcher) {
        // Arrange
        val response = CompletableDeferred<AuthResult<Unit>>()
        coEvery { resendSignUpCodeUseCase(any()) } coAnswers { response.await() }
        runCurrent()
        testScheduler.advanceTimeBy(60_000L)
        runCurrent()
        Assertions.assertTrue(viewModel.isResendEnabled)

        // Act
        // Coroutine/Interaction: a pending resend disables the flag until success starts a new cooldown
        viewModel.onResend()
        viewModel.onResend()
        runCurrent()
        val enabledWhileLoading = viewModel.isResendEnabled
        val loadingDuringRequest = viewModel.uiState.value.isLoading
        response.complete(AuthResult.Success(Unit))
        runCurrent()

        // Assert
        Assertions.assertFalse(enabledWhileLoading)
        Assertions.assertTrue(loadingDuringRequest)
        coVerify(exactly = 1) { resendSignUpCodeUseCase(any()) }
        Assertions.assertFalse(viewModel.uiState.value.isLoading)
        Assertions.assertEquals(60, viewModel.uiState.value.resendTimerSeconds)
        Assertions.assertFalse(viewModel.isResendEnabled)
    }

    @Test
    fun `onVerify emits queued navigation when collection starts after completion`() = runTest(testDispatcher) {
        // Arrange
        coEvery { confirmSignUpUseCase(any()) } returns AuthResult.Success(Unit)

        // Act
        // Flow: automatic confirmation completes before a Screen collector subscribes
        fillCode("123456")
        runCurrent()

        // Assert
        viewModel.effect.test {
            Assertions.assertEquals(VerificationEffect.NavigateToHome, awaitItem())
            runCurrent()
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
        Assertions.assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 1) { confirmSignUpUseCase(any()) }
    }

    @Test
    fun `onVerify emits only queued effects when collection restarts`() = runTest(testDispatcher) {
        // Arrange
        coEvery { confirmSignUpUseCase(any()) } returnsMany listOf(
            AuthResult.Error(AuthError.Network()),
            AuthResult.Error(AuthError.TooManyRequests())
        )
        viewModel.effect.test {
            fillCode("123456")
            runCurrent()
            Assertions.assertEquals(VerificationEffect.ShowSnackbar(AuthSnackbarMessage.Network), awaitItem())
            cancelAndConsumeRemainingEvents()
        }

        // Act
        // Flow: the new confirmation result waits during collector absence without replaying the previous result
        viewModel.onVerify()
        runCurrent()

        // Assert
        viewModel.effect.test {
            Assertions.assertEquals(VerificationEffect.ShowSnackbar(AuthSnackbarMessage.TooManyRequests), awaitItem())
            runCurrent()
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
        viewModel.effect.test {
            runCurrent()
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
        coVerify(exactly = 2) { confirmSignUpUseCase(any()) }
    }

    @Test
    fun `init emits NavigateBack once when email is missing before collection starts`() = runTest(testDispatcher) {
        // Arrange
        val initializedViewModel = VerificationCodeViewModel(
            SavedStateHandle(),
            confirmSignUpUseCase,
            resendSignUpCodeUseCase
        )

        // Act
        // Flow: invalid navigation arguments queue the initial return before the first collector is ready
        runCurrent()

        // Assert
        Assertions.assertNull(initializedViewModel.email)
        initializedViewModel.effect.test {
            Assertions.assertEquals(VerificationEffect.NavigateBack, awaitItem())
            runCurrent()
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
        initializedViewModel.effect.test {
            runCurrent()
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
        coVerify(exactly = 0) { confirmSignUpUseCase(any()) }
        coVerify(exactly = 0) { resendSignUpCodeUseCase(any()) }
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
        val resendTimerSeconds: Int = 60
    ) {
        companion object {
            fun from(state: com.appvoyager.cloudphotos.ui.auth.uistate.VerificationCodeUiState) =
                VerificationCodeSnapshot(
                    codes = state.codes,
                    isLoading = state.isLoading,
                    codeError = state.codeError,
                    resendTimerSeconds = state.resendTimerSeconds
                )
        }
    }
}
