package com.appvoyager.cloudphotos.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.di.AuthRepositoryModule
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.model.AuthState
import com.appvoyager.cloudphotos.domain.auth.model.AuthUser
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmSignUpRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResendSignUpCodeRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignInRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignUpRequest
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.appvoyager.cloudphotos.core.ui.R as CoreUiR

@HiltAndroidTest
@UninstallModules(AuthRepositoryModule::class)
@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val repository = DeferredAuthRepository()

    @BindValue
    @JvmField
    val authRepository: AuthRepository = repository

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        hilt.inject()
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun mainActivity_setsLoginDestination_whenDelayedInitialSessionReturnsGuest() {
        // Arrange
        val session = repository.enqueueSession()
        launchActivity()
        compose.onRoot().assertExists()

        // Act
        // Flow: complete the initial request only after the None composition has settled
        session.complete(AuthResult.Success(AuthSession(AuthState.Guest)))

        // Assert
        compose.onNode(hasText(text(CoreUiR.string.login_sign_in)) and hasClickAction()).assertIsDisplayed()
    }

    @Test
    fun mainActivity_setsRetryEnabled_whenDelayedRetryReturnsSameError() {
        // Arrange
        val initialSession = repository.enqueueSession()
        initialSession.complete(AuthResult.Error(AuthError.Network()))
        val retrySession = repository.enqueueSession()
        launchActivity()
        val retry = compose.onNode(hasText(text(R.string.retry)) and hasClickAction())
        retry.assertIsEnabled()

        // Act
        // Flow: isRetrying must update the button while uiState remains SessionCheckError
        retry.performClick()

        // Assert
        retry.assertIsNotEnabled()

        // Act & Assert
        // Flow: the same error result must release the retry button without a uiState change
        retrySession.complete(AuthResult.Error(AuthError.Network()))
        retry.assertIsEnabled()
    }

    private fun launchActivity() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
    }

    private fun text(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private class DeferredAuthRepository : AuthRepository {
        private val sessions = ConcurrentLinkedQueue<CompletableDeferred<AuthResult<AuthSession>>>()

        fun enqueueSession(): CompletableDeferred<AuthResult<AuthSession>> =
            CompletableDeferred<AuthResult<AuthSession>>().also { sessions.add(it) }

        override suspend fun getSession(): AuthResult<AuthSession> =
            checkNotNull(sessions.poll()) { "Unexpected session request" }.await()

        override suspend fun signUp(request: SignUpRequest): AuthResult<Unit> = error("Unexpected sign up")

        override suspend fun confirmSignUp(request: ConfirmSignUpRequest): AuthResult<Unit> =
            error("Unexpected sign up confirmation")

        override suspend fun signIn(request: SignInRequest): AuthResult<SignInState> = error("Unexpected sign in")

        override suspend fun signOut(): AuthResult<Unit> = error("Unexpected sign out")

        override suspend fun fetchCurrentUser(): AuthResult<AuthUser> = error("Unexpected current user request")

        override suspend fun resendSignUpCode(request: ResendSignUpCodeRequest): AuthResult<Unit> =
            error("Unexpected sign up code request")

        override suspend fun resetPassword(request: ResetPasswordRequest): AuthResult<Unit> =
            error("Unexpected password reset")

        override suspend fun confirmResetPassword(request: ConfirmResetPasswordRequest): AuthResult<Unit> =
            error("Unexpected password reset confirmation")

        override suspend fun deleteUser(): AuthResult<Unit> = error("Unexpected user deletion")
    }
}
