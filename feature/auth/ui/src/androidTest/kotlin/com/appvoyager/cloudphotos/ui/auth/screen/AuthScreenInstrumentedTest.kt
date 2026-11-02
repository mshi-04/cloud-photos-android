package com.appvoyager.cloudphotos.ui.auth.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
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
import com.appvoyager.cloudphotos.ui.auth.testutil.ScreenAuthRepository
import com.appvoyager.cloudphotos.ui.auth.testutil.ScreenViewModelFactory
import com.appvoyager.cloudphotos.ui.auth.viewmodel.ForgotPasswordViewModel
import com.appvoyager.cloudphotos.ui.auth.viewmodel.ResetPasswordViewModel
import com.appvoyager.cloudphotos.ui.auth.viewmodel.VerificationCodeViewModel
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AuthScreenInstrumentedTest {

    @get:Rule
    val compose = createComposeRule()

    private val scheduler = TestCoroutineScheduler()
    private val factory = ScreenViewModelFactory(scheduler)
    private val repository = ScreenAuthRepository()
    private val lifecycleOwner = ScreenLifecycleOwner()
    private val revision = mutableIntStateOf(0)

    @After
    fun tearDown() {
        compose.runOnIdle {
            lifecycleOwner.registry.currentState = Lifecycle.State.DESTROYED
            factory.close()
            scheduler.runCurrent()
        }
    }

    @Test
    fun forgotPasswordScreen_setsSubmitEnabled_whenCollectedEmailRemainsValid() {
        // Arrange
        val viewModel = factory.create { ForgotPasswordViewModel(ResetPasswordUseCase(repository)) }
        viewModel.onEmailChanged("test@example.com")
        setScreen { revision ->
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateToResetPassword = { error("Unexpected navigation") },
                onNavigateToVerification = { error("Unexpected navigation") },
                onNavigateBack = { error("Unexpected navigation $revision") }
            )
        }
        val submit = compose.onNodeWithText(text(R.string.forgot_password_send_code))
        submit.assertIsEnabled()

        // Act
        // State/Flow: force Screen recomposition while lifecycle collection retains the displayed email
        updateCurrentState { viewModel.onClearEmail() }

        // Assert
        assertFalse(viewModel.isFormValid)
        compose.onNodeWithText("test@example.com").assertTextContains("test@example.com")
        submit.assertIsEnabled()

        // Act & Assert
        // Flow: restarting collection applies the current empty email and disables submit
        resumeCollection()
        submit.assertIsNotEnabled()
    }

    @Test
    fun resetPasswordScreen_setsConfirmEnabled_whenCollectedCodeRemainsComplete() {
        // Arrange
        val viewModel = createResetViewModel()
        viewModel.onCodeChanged(0, "123456")
        viewModel.onNewPasswordChanged("password123")
        setResetScreen(viewModel)
        val confirm = compose.onNodeWithText(text(R.string.reset_password_button))
        confirm.assertIsEnabled()

        // Act
        // State/Flow: the current code changes without replacing the collected completed code
        updateCurrentState { viewModel.onCodeChanged(2, "") }

        // Assert
        assertFalse(viewModel.isFormValid)
        codeField(3).assertTextContains("3")
        confirm.assertIsEnabled()

        // Act & Assert
        // Flow: collection resumes with the incomplete code and disables confirm
        resumeCollection()
        codeField(3).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        confirm.assertIsNotEnabled()
    }

    @Test
    fun resetPasswordScreen_setsResendDisabled_whenCollectedTimerHasNotExpired() {
        // Arrange
        val viewModel = createResetViewModel()
        setResetScreen(viewModel)
        val countdown = text(R.string.reset_password_resend_timer, 60)
        compose.onNodeWithText(countdown).assertIsNotEnabled()

        // Act
        // Coroutine/Flow: only the ViewModel timer reaches zero while the displayed timer stays at sixty
        updateCurrentState { scheduler.advanceTimeBy(60_000L) }

        // Assert
        assertTrue(viewModel.isResendEnabled)
        compose.onNodeWithText(countdown).assertIsNotEnabled()

        // Act & Assert
        // Flow: the new zero-second snapshot updates both the label and resend availability
        resumeCollection()
        compose.onNodeWithText(text(R.string.reset_password_resend)).assertIsEnabled()
    }

    @Test
    fun verificationCodeScreen_setsVerifyEnabled_whenCollectedCodeRemainsComplete() {
        // Arrange
        val viewModel = createVerificationViewModel()
        viewModel.onCodeChanged(0, "123456")
        scheduler.runCurrent()
        setVerificationScreen(viewModel)
        val verify = compose.onNodeWithText(text(R.string.verification_code_verify))
        verify.assertIsEnabled()

        // Act
        // State/Flow: the displayed complete code keeps its enabled flag after the current code changes
        updateCurrentState { viewModel.onCodeChanged(2, "") }

        // Assert
        assertFalse(viewModel.isCodeComplete)
        codeField(3).assertTextContains("3")
        verify.assertIsEnabled()

        // Act & Assert
        // Flow: the current incomplete code disables verification once collection restarts
        resumeCollection()
        codeField(3).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        verify.assertIsNotEnabled()
    }

    @Test
    fun verificationCodeScreen_setsResendDisabled_whenCollectedTimerHasNotExpired() {
        // Arrange
        val viewModel = createVerificationViewModel()
        setVerificationScreen(viewModel)
        val countdown = text(R.string.verification_code_resend_timer, 60)
        compose.onNodeWithText(countdown).assertIsNotEnabled()

        // Act
        // Coroutine/Flow: a newer timer value must not enable resend beside the collected countdown
        updateCurrentState { scheduler.advanceTimeBy(60_000L) }

        // Assert
        assertTrue(viewModel.isResendEnabled)
        compose.onNodeWithText(countdown).assertIsNotEnabled()

        // Act & Assert
        // Flow: restarting collection displays the resend label with its enabled state
        resumeCollection()
        compose.onNodeWithText(text(R.string.verification_code_resend)).assertIsEnabled()
    }

    private fun createResetViewModel(): ResetPasswordViewModel = factory.create {
        ResetPasswordViewModel(
            SavedStateHandle(mapOf("email" to "test@example.com")),
            ConfirmResetPasswordUseCase(repository),
            ResetPasswordUseCase(repository)
        )
    }

    private fun createVerificationViewModel(): VerificationCodeViewModel = factory.create {
        VerificationCodeViewModel(
            SavedStateHandle(mapOf("email" to "test@example.com")),
            ConfirmSignUpUseCase(repository),
            ResendSignUpCodeUseCase(repository)
        )
    }

    private fun setResetScreen(viewModel: ResetPasswordViewModel) = setScreen { revision ->
        ResetPasswordScreen(viewModel) { error("Unexpected navigation $revision") }
    }

    private fun setVerificationScreen(viewModel: VerificationCodeViewModel) = setScreen { revision ->
        VerificationCodeScreen(
            viewModel,
            onNavigateToHome = { error("Unexpected navigation $revision") },
            onNavigateBack = { error("Unexpected navigation") }
        )
    }

    private fun setScreen(content: @Composable (Int) -> Unit) {
        compose.runOnIdle { lifecycleOwner.registry.currentState = Lifecycle.State.CREATED }
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                CloudPhotosTheme { content(revision.intValue) }
            }
        }
        compose.runOnIdle { scheduler.runCurrent() }
    }

    private fun updateCurrentState(update: () -> Unit) {
        compose.runOnIdle {
            update()
            scheduler.runCurrent()
            revision.intValue++
        }
        compose.waitForIdle()
    }

    private fun resumeCollection() {
        compose.runOnIdle { lifecycleOwner.registry.currentState = Lifecycle.State.STARTED }
        compose.waitForIdle()
    }

    private fun codeField(position: Int) =
        compose.onNodeWithContentDescription(text(R.string.code_input_box_description, position))

    private fun text(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private class ScreenLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = registry
    }
}
