package com.appvoyager.cloudphotos.ui.auth.viewmodel

import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.usecase.ResetPasswordUseCase
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.ui.auth.effect.ForgotPasswordEffect
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `initial state has empty email`() {
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertFalse(state.isLoading)
        assertNull(state.emailError)
    }

    @Test
    fun `onEmailChanged updates email and clears error`() {
        viewModel.onEmailChanged("test@example.com")
        val state = viewModel.uiState.value
        assertEquals("test@example.com", state.email)
        assertNull(state.emailError)
    }

    @Test
    fun `onClearEmail resets email`() {
        viewModel.onEmailChanged("test@example.com")
        viewModel.onClearEmail()
        assertEquals("", viewModel.uiState.value.email)
    }

    @Test
    fun `onSubmit success emits NavigateToResetCode`() = runTest(testDispatcher) {
        viewModel.onEmailChanged("test@example.com")
        coEvery { resetPasswordUseCase(any()) } returns AuthResult.Success(Unit)

        var effect: ForgotPasswordEffect? = null
        val job = launch { effect = viewModel.effect.first() }
        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(effect is ForgotPasswordEffect.NavigateToResetCode)
        assertEquals(
            Email.of("test@example.com"),
            (effect as ForgotPasswordEffect.NavigateToResetCode).email
        )
        assertFalse(viewModel.uiState.value.isLoading)
        job.cancel()
    }

    @Test
    fun `onSubmit with Network error emits ShowSnackbar with error_network`() =
        runTest(testDispatcher) {
            viewModel.onEmailChanged("test@example.com")
            coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
                AuthError.Network()
            )

            var effect: ForgotPasswordEffect? = null
            val job = launch { effect = viewModel.effect.first() }
            viewModel.onSubmit()
            advanceUntilIdle()

            assertTrue(effect is ForgotPasswordEffect.ShowSnackbar)
            assertEquals(
                R.string.error_network,
                (effect as ForgotPasswordEffect.ShowSnackbar).messageResId
            )
            job.cancel()
        }

    @Test
    fun `onSubmit with TooManyRequests emits ShowSnackbar with error_too_many_requests`() =
        runTest(testDispatcher) {
            viewModel.onEmailChanged("test@example.com")
            coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
                AuthError.TooManyRequests()
            )

            var effect: ForgotPasswordEffect? = null
            val job = launch { effect = viewModel.effect.first() }
            viewModel.onSubmit()
            advanceUntilIdle()

            assertTrue(effect is ForgotPasswordEffect.ShowSnackbar)
            assertEquals(
                R.string.error_too_many_requests,
                (effect as ForgotPasswordEffect.ShowSnackbar).messageResId
            )
            job.cancel()
        }

    @Test
    fun `onSubmit with UserNotConfirmed emits ShowSnackbar then NavigateToVerification`() =
        runTest(testDispatcher) {
            viewModel.onEmailChanged("test@example.com")
            coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
                AuthError.UserNotConfirmed()
            )

            val effects = mutableListOf<ForgotPasswordEffect>()
            val job = launch {
                viewModel.effect.collect { effects.add(it) }
            }
            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(2, effects.size)
            assertTrue(effects[0] is ForgotPasswordEffect.ShowSnackbar)
            assertEquals(
                R.string.error_user_not_confirmed,
                (effects[0] as ForgotPasswordEffect.ShowSnackbar).messageResId
            )
            assertTrue(effects[1] is ForgotPasswordEffect.NavigateToVerification)
            assertEquals(
                Email.of("test@example.com"),
                (effects[1] as ForgotPasswordEffect.NavigateToVerification).email
            )
            job.cancel()
        }

    @Test
    fun `onSubmit with InvalidCredentials emits ShowSnackbar with error_invalid_credentials`() =
        runTest(testDispatcher) {
            viewModel.onEmailChanged("test@example.com")
            coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
                AuthError.InvalidCredentials()
            )

            var effect: ForgotPasswordEffect? = null
            val job = launch { effect = viewModel.effect.first() }
            viewModel.onSubmit()
            advanceUntilIdle()

            assertTrue(effect is ForgotPasswordEffect.ShowSnackbar)
            assertEquals(
                R.string.error_invalid_credentials,
                (effect as ForgotPasswordEffect.ShowSnackbar).messageResId
            )
            job.cancel()
        }

    @Test
    fun `onSubmit with InvalidPassword emits ShowSnackbar with error_invalid_password`() =
        runTest(testDispatcher) {
            viewModel.onEmailChanged("test@example.com")
            coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
                AuthError.InvalidPassword()
            )

            var effect: ForgotPasswordEffect? = null
            val job = launch { effect = viewModel.effect.first() }
            viewModel.onSubmit()
            advanceUntilIdle()

            assertTrue(effect is ForgotPasswordEffect.ShowSnackbar)
            assertEquals(
                R.string.error_invalid_password,
                (effect as ForgotPasswordEffect.ShowSnackbar).messageResId
            )
            job.cancel()
        }

    @Test
    fun `onSubmit with CodeExpired emits ShowSnackbar with error_code_expired`() =
        runTest(testDispatcher) {
            viewModel.onEmailChanged("test@example.com")
            coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
                AuthError.CodeExpired()
            )

            var effect: ForgotPasswordEffect? = null
            val job = launch { effect = viewModel.effect.first() }
            viewModel.onSubmit()
            advanceUntilIdle()

            assertTrue(effect is ForgotPasswordEffect.ShowSnackbar)
            assertEquals(
                R.string.error_code_expired,
                (effect as ForgotPasswordEffect.ShowSnackbar).messageResId
            )
            job.cancel()
        }

    @Test
    fun `onSubmit with CodeMismatch emits ShowSnackbar with error_code_mismatch`() =
        runTest(testDispatcher) {
            viewModel.onEmailChanged("test@example.com")
            coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
                AuthError.CodeMismatch()
            )

            var effect: ForgotPasswordEffect? = null
            val job = launch { effect = viewModel.effect.first() }
            viewModel.onSubmit()
            advanceUntilIdle()

            assertTrue(effect is ForgotPasswordEffect.ShowSnackbar)
            assertEquals(
                R.string.error_code_mismatch,
                (effect as ForgotPasswordEffect.ShowSnackbar).messageResId
            )
            job.cancel()
        }

    @Test
    fun `onSubmit with Unknown error emits ShowSnackbar with error_unknown`() =
        runTest(testDispatcher) {
            viewModel.onEmailChanged("test@example.com")
            coEvery { resetPasswordUseCase(any()) } returns AuthResult.Error(
                AuthError.Unknown()
            )

            var effect: ForgotPasswordEffect? = null
            val job = launch { effect = viewModel.effect.first() }
            viewModel.onSubmit()
            advanceUntilIdle()

            assertTrue(effect is ForgotPasswordEffect.ShowSnackbar)
            assertEquals(
                R.string.error_unknown,
                (effect as ForgotPasswordEffect.ShowSnackbar).messageResId
            )
            job.cancel()
        }
}
