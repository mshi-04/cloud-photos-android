package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.usecase.ConfirmSignUpUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.ResendSignUpCodeUseCase
import com.appvoyager.cloudphotos.ui.auth.effect.VerificationEffect
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

    private fun createViewModel(
        email: String = "test@example.com"
    ): VerificationCodeViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf("email" to email)
        )
        return VerificationCodeViewModel(
            savedStateHandle = savedStateHandle,
            confirmSignUpUseCase = confirmSignUpUseCase,
            resendSignUpCodeUseCase = resendSignUpCodeUseCase,
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
    fun `initial state has empty codes and timer started`() {
        Assertions.assertEquals(List(6) { "" }, viewModel.codes)
        Assertions.assertFalse(viewModel.isLoading)
        Assertions.assertNull(viewModel.codeError)
        Assertions.assertEquals(60, viewModel.resendTimerSeconds)
        Assertions.assertFalse(viewModel.isResendEnabled)
    }

    @Test
    fun `onCodeChanged updates single digit`() {
        viewModel.onCodeChanged(0, "1")
        Assertions.assertEquals("1", viewModel.codes[0])
        Assertions.assertEquals("", viewModel.codes[1])
    }

    @Test
    fun `onCodeChanged with paste distributes digits`() {
        viewModel.onCodeChanged(0, "123456")
        Assertions.assertEquals(listOf("1", "2", "3", "4", "5", "6"), viewModel.codes)
    }

    @Test
    fun `isCodeComplete returns true when all 6 digits filled`() {
        repeat(6) { i -> viewModel.onCodeChanged(i, (i + 1).toString()) }
        Assertions.assertTrue(viewModel.isCodeComplete)
    }

    @Test
    fun `onVerify success navigates to home`() = runTest(testDispatcher) {
        // Arrange
        coEvery { confirmSignUpUseCase(any()) } returns AuthResult.Success(Unit)

        viewModel.onCodeChanged(0, "1")
        viewModel.onCodeChanged(1, "2")
        viewModel.onCodeChanged(2, "3")
        viewModel.onCodeChanged(3, "4")
        viewModel.onCodeChanged(4, "5")

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
    fun `onVerify with CodeMismatch sets codeError`() = runTest(testDispatcher) {
        // Arrange
        coEvery { confirmSignUpUseCase(any()) } returns AuthResult.Error(
            AuthError.CodeMismatch("wrong code")
        )

        viewModel.onCodeChanged(0, "1")
        viewModel.onCodeChanged(1, "2")
        viewModel.onCodeChanged(2, "3")
        viewModel.onCodeChanged(3, "4")
        viewModel.onCodeChanged(4, "5")

        // Act
        viewModel.onCodeChanged(5, "6")
        advanceUntilIdle()

        // Assert
        Assertions.assertEquals("確認コードが正しくありません", viewModel.codeError)
    }

    @Test
    fun `onVerify with CodeExpired sets codeError with resend prompt`() =
        runTest(testDispatcher) {
            // Arrange
            coEvery { confirmSignUpUseCase(any()) } returns AuthResult.Error(
                AuthError.CodeExpired("expired")
            )

            viewModel.onCodeChanged(0, "1")
            viewModel.onCodeChanged(1, "2")
            viewModel.onCodeChanged(2, "3")
            viewModel.onCodeChanged(3, "4")
            viewModel.onCodeChanged(4, "5")

            // Act
            viewModel.onCodeChanged(5, "6")
            advanceUntilIdle()

            // Assert
            Assertions.assertEquals(
                "確認コードの有効期限が切れました。再送信してください",
                viewModel.codeError
            )
        }

    @Test
    fun `onVerify with Network error emits ShowSnackbar`() = runTest(testDispatcher) {
        // Arrange
        coEvery { confirmSignUpUseCase(any()) } returns AuthResult.Error(
            AuthError.Network("offline")
        )

        viewModel.onCodeChanged(0, "1")
        viewModel.onCodeChanged(1, "2")
        viewModel.onCodeChanged(2, "3")
        viewModel.onCodeChanged(3, "4")
        viewModel.onCodeChanged(4, "5")

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
    fun `onResend success emits ShowSnackbar and resets timer`() = runTest(testDispatcher) {
        // Arrange
        coEvery { resendSignUpCodeUseCase(any()) } returns AuthResult.Success(Unit)

        repeat(61) {
            testDispatcher.scheduler.advanceTimeBy(1_000L)
            testDispatcher.scheduler.runCurrent()
        }

        Assertions.assertTrue(
            viewModel.resendTimerSeconds <= 0,
            "Timer should have elapsed, but was ${viewModel.resendTimerSeconds}"
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
            "確認コードを再送信しました",
            (effect as VerificationEffect.ShowSnackbar).message
        )
        Assertions.assertTrue(viewModel.resendTimerSeconds > 0, "Timer should have been reset")
        job.cancel()
    }

}