package com.appvoyager.cloudphotos.ui.auth.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appvoyager.cloudphotos.core.ui.R
import com.appvoyager.cloudphotos.domain.auth.usecase.ConfirmResetPasswordUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.ConfirmSignUpUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.ResendSignUpCodeUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.ResetPasswordUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.SignInUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.SignUpUseCase
import com.appvoyager.cloudphotos.ui.auth.effect.AuthSnackbarMessage
import com.appvoyager.cloudphotos.ui.auth.testutil.LifecycleAuthRepository
import com.appvoyager.cloudphotos.ui.auth.testutil.ScreenViewModelFactory
import com.appvoyager.cloudphotos.ui.auth.viewmodel.ForgotPasswordViewModel
import com.appvoyager.cloudphotos.ui.auth.viewmodel.LoginViewModel
import com.appvoyager.cloudphotos.ui.auth.viewmodel.ResetPasswordViewModel
import com.appvoyager.cloudphotos.ui.auth.viewmodel.VerificationCodeViewModel
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AuthEffectLifecycleInstrumentedTest {

    @get:Rule
    val compose = createComposeRule()

    private val scheduler = TestCoroutineScheduler()
    private val factory = ScreenViewModelFactory(scheduler)
    private val repository = LifecycleAuthRepository()
    private var lifecycleOwner by mutableStateOf(ScreenLifecycleOwner())
    private val callbackVersion = mutableIntStateOf(0)
    private val compositionGeneration = mutableIntStateOf(0)

    @After
    fun tearDown() {
        compose.runOnIdle {
            lifecycleOwner.registry.currentState = Lifecycle.State.DESTROYED
            factory.close()
            scheduler.runCurrent()
        }
    }

    @Test
    fun loginScreen_callsLatestCallbackOnce_whenSignInCompletesWhileStopped() {
        // Arrange
        val viewModel = createLoginViewModel()
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")
        val callbacks = mutableListOf<Int>()
        setScreen { version ->
            LoginScreen(
                viewModel,
                onNavigateToVerification = { error("Unexpected verification") },
                onNavigateToHome = { callbacks += version },
                onNavigateToForgotPassword = { error("Unexpected password reset") }
            )
        }
        setLifecycle(Lifecycle.State.STARTED)
        setLifecycle(Lifecycle.State.CREATED)

        // Act
        // Lifecycle/Flow: an unreceived completion waits while stopped and uses the recomposed callback
        runViewModel { viewModel.onSignIn() }
        updateCallback()

        // Assert
        assertEquals(emptyList<Int>(), callbacks)
        setLifecycle(Lifecycle.State.STARTED)
        assertEquals(listOf(1), callbacks)
        restartCollection()
        assertEquals(listOf(1), callbacks)
    }

    @Test
    fun forgotPasswordScreen_callsResetCallbackOnce_whenRequestCompletesWhileStopped() {
        // Arrange
        val viewModel = factory.create { ForgotPasswordViewModel(ResetPasswordUseCase(repository)) }
        viewModel.onEmailChanged("test@example.com")
        val callbacks = mutableListOf<Pair<Int, String>>()
        setScreen { version ->
            ForgotPasswordScreen(
                viewModel,
                onNavigateToResetPassword = { callbacks += version to it.value },
                onNavigateToVerification = { error("Unexpected verification") },
                onNavigateBack = { error("Unexpected back navigation") }
            )
        }
        setLifecycle(Lifecycle.State.STARTED)
        setLifecycle(Lifecycle.State.CREATED)

        // Act
        // Lifecycle/Flow: stopping collection retains the reset result without invoking navigation
        runViewModel { viewModel.onSubmit() }
        updateCallback()

        // Assert
        assertEquals(emptyList<Pair<Int, String>>(), callbacks)
        setLifecycle(Lifecycle.State.STARTED)
        assertEquals(listOf(1 to "test@example.com"), callbacks)
        restartCollection()
        assertEquals(listOf(1 to "test@example.com"), callbacks)
    }

    @Test
    fun resetPasswordScreen_callsLoginCallbackOnce_whenConfirmationCompletesWhileStopped() {
        // Arrange
        val viewModel = createResetViewModel()
        viewModel.onCodeChanged(0, "123456")
        viewModel.onNewPasswordChanged("password123")
        val callbacks = mutableListOf<Pair<Int, AuthSnackbarMessage?>>()
        setScreen { version -> ResetPasswordScreen(viewModel) { callbacks += version to it } }
        setLifecycle(Lifecycle.State.STARTED)
        setLifecycle(Lifecycle.State.CREATED)

        // Act
        // Lifecycle/Flow: the confirmation result waits for STARTED and retains its Snackbar argument
        runViewModel { viewModel.onConfirm() }
        updateCallback()

        // Assert
        assertEquals(emptyList<Pair<Int, AuthSnackbarMessage?>>(), callbacks)
        setLifecycle(Lifecycle.State.STARTED)
        assertEquals(listOf(1 to AuthSnackbarMessage.PasswordReset), callbacks)
        restartCollection()
        assertEquals(listOf(1 to AuthSnackbarMessage.PasswordReset), callbacks)
    }

    @Test
    fun verificationCodeScreen_callsBackOnce_whenEmailMissingBeforeCollectionStarts() {
        // Arrange
        val viewModel = createVerificationViewModel(SavedStateHandle())
        scheduler.runCurrent()
        val callbacks = mutableListOf<Int>()
        setScreen { version ->
            VerificationCodeScreen(
                viewModel,
                onNavigateToHome = { error("Unexpected home navigation") },
                onNavigateBack = { callbacks += version }
            )
        }
        updateCallback()

        // Act
        // Lifecycle/Flow: the init result queued before composition is first delivered on STARTED
        assertEquals(emptyList<Int>(), callbacks)
        setLifecycle(Lifecycle.State.STARTED)

        // Assert
        assertEquals(listOf(1), callbacks)
        restartCollection()
        assertEquals(listOf(1), callbacks)
    }

    @Test
    fun verificationCodeScreen_callsHomeOnce_whenLifecycleOwnerIsReplacedWithPendingCompletion() {
        // Arrange
        val viewModel = createVerificationViewModel()
        val callbacks = mutableListOf<Int>()
        setScreen { version ->
            VerificationCodeScreen(
                viewModel,
                onNavigateToHome = { callbacks += version },
                onNavigateBack = { error("Unexpected back navigation") }
            )
        }
        setLifecycle(Lifecycle.State.STARTED)
        setLifecycle(Lifecycle.State.CREATED)
        runViewModel { viewModel.onCodeChanged(0, "123456") }
        assertEquals(emptyList<Int>(), callbacks)

        // Act
        // Lifecycle/Flow: replace the owner while retaining the ViewModel and its unreceived result
        compose.runOnIdle {
            lifecycleOwner.registry.currentState = Lifecycle.State.DESTROYED
            lifecycleOwner = ScreenLifecycleOwner().also { it.registry.currentState = Lifecycle.State.CREATED }
            callbackVersion.intValue++
        }
        compose.waitForIdle()
        setLifecycle(Lifecycle.State.STARTED)

        // Assert
        assertEquals(listOf(1), callbacks)
        restartCollection()
        assertEquals(listOf(1), callbacks)
    }

    @Test
    fun loginScreen_setsInitialSnackbarOnce_whenCollectionStartsAfterInit() {
        // Arrange
        val handle = SavedStateHandle(mapOf("message" to AuthSnackbarMessage.PasswordReset.key))
        val viewModel = createLoginViewModel(handle)
        scheduler.runCurrent()
        val message = text(R.string.message_password_reset)
        setScreen {
            LoginScreen(
                viewModel,
                onNavigateToVerification = { error("Unexpected verification") },
                onNavigateToHome = { error("Unexpected home navigation") },
                onNavigateToForgotPassword = { error("Unexpected password reset") }
            )
        }
        assertFalse(handle.contains("message"))
        compose.onNodeWithText(message).assertDoesNotExist()

        // Act
        // Lifecycle/Flow: initial message is queued before the collector exists and shown at first STARTED
        setLifecycle(Lifecycle.State.STARTED)

        // Assert
        compose.onNodeWithText(message).assertExists()

        // Act & Assert
        // Lifecycle/Flow: cancelling a received Snackbar and remounting the same ViewModel does not replay it
        setLifecycle(Lifecycle.State.CREATED)
        recreateComposition()
        setLifecycle(Lifecycle.State.STARTED)
        compose.onNodeWithText(message).assertDoesNotExist()
    }

    @Test
    fun resetPasswordScreen_setsRemainingCooldown_whenCollectionAndCompositionRestart() {
        // Arrange
        val viewModel = createResetViewModel()
        setScreen { ResetPasswordScreen(viewModel) { error("Unexpected navigation") } }
        setLifecycle(Lifecycle.State.STARTED)
        runViewModel { scheduler.advanceTimeBy(15_000L) }
        compose.onNodeWithText(text(R.string.reset_password_resend_timer, 45)).assertExists()

        // Act
        // Lifecycle/Coroutine: time continues while stopped and the retained ViewModel survives a new composition
        setLifecycle(Lifecycle.State.CREATED)
        runViewModel { scheduler.advanceTimeBy(10_000L) }
        recreateComposition()
        setLifecycle(Lifecycle.State.STARTED)

        // Assert
        assertEquals(35, viewModel.uiState.value.resendTimerSeconds)
        compose.onNodeWithText(text(R.string.reset_password_resend_timer, 35)).assertExists()
        runViewModel { scheduler.advanceTimeBy(35_000L) }
        compose.onNodeWithText(text(R.string.reset_password_resend)).assertIsEnabled()
    }

    @Test
    fun verificationCodeScreen_setsRemainingCooldown_whenCollectionAndCompositionRestart() {
        // Arrange
        val viewModel = createVerificationViewModel()
        setScreen {
            VerificationCodeScreen(
                viewModel,
                onNavigateToHome = { error("Unexpected home navigation") },
                onNavigateBack = { error("Unexpected back navigation") }
            )
        }
        setLifecycle(Lifecycle.State.STARTED)
        runViewModel { scheduler.advanceTimeBy(15_000L) }
        compose.onNodeWithText(text(R.string.verification_code_resend_timer, 45)).assertExists()

        // Act
        // Lifecycle/Coroutine: collector restart and remount must not restart the verification cooldown
        setLifecycle(Lifecycle.State.CREATED)
        runViewModel { scheduler.advanceTimeBy(10_000L) }
        recreateComposition()
        setLifecycle(Lifecycle.State.STARTED)

        // Assert
        assertEquals(35, viewModel.uiState.value.resendTimerSeconds)
        compose.onNodeWithText(text(R.string.verification_code_resend_timer, 35)).assertExists()
        runViewModel { scheduler.advanceTimeBy(35_000L) }
        compose.onNodeWithText(text(R.string.verification_code_resend)).assertIsEnabled()
    }

    private fun createLoginViewModel(handle: SavedStateHandle = SavedStateHandle()): LoginViewModel = factory.create {
        LoginViewModel(handle, SignInUseCase(repository), SignUpUseCase(repository))
    }

    private fun createResetViewModel(): ResetPasswordViewModel = factory.create {
        ResetPasswordViewModel(
            SavedStateHandle(mapOf("email" to "test@example.com")),
            ConfirmResetPasswordUseCase(repository),
            ResetPasswordUseCase(repository)
        )
    }

    private fun createVerificationViewModel(
        handle: SavedStateHandle = SavedStateHandle(mapOf("email" to "test@example.com"))
    ): VerificationCodeViewModel = factory.create {
        VerificationCodeViewModel(handle, ConfirmSignUpUseCase(repository), ResendSignUpCodeUseCase(repository))
    }

    private fun setScreen(content: @Composable (Int) -> Unit) {
        compose.runOnIdle { lifecycleOwner.registry.currentState = Lifecycle.State.CREATED }
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                CloudPhotosTheme {
                    key(compositionGeneration.intValue) { content(callbackVersion.intValue) }
                }
            }
        }
        runViewModel { }
    }

    private fun runViewModel(action: () -> Unit) {
        compose.runOnIdle {
            action()
            scheduler.runCurrent()
        }
        compose.waitForIdle()
    }

    private fun setLifecycle(state: Lifecycle.State) {
        compose.runOnIdle { lifecycleOwner.registry.currentState = state }
        compose.waitForIdle()
    }

    private fun restartCollection() {
        setLifecycle(Lifecycle.State.CREATED)
        setLifecycle(Lifecycle.State.STARTED)
    }

    private fun updateCallback() {
        compose.runOnIdle { callbackVersion.intValue++ }
        compose.waitForIdle()
    }

    private fun recreateComposition() {
        compose.runOnIdle { compositionGeneration.intValue++ }
        compose.waitForIdle()
        runViewModel { }
    }

    private fun text(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private class ScreenLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = registry
    }
}
