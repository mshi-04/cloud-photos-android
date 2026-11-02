package com.appvoyager.cloudphotos.ui

import app.cash.turbine.test
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.model.AuthState
import com.appvoyager.cloudphotos.domain.auth.usecase.GetSessionUseCase
import com.appvoyager.cloudphotos.fcm.FcmTokenRegistrar
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getSessionUseCase = mockk<GetSessionUseCase>()
    private val fcmTokenRegistrar = mockk<FcmTokenRegistrar>()
    private lateinit var viewModel: MainViewModel

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        Dispatchers.setMain(testDispatcher)
        every { fcmTokenRegistrar.register() } just runs
        viewModel = MainViewModel(getSessionUseCase, fcmTokenRegistrar)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState returns initial snapshot when viewModel is created`() {
        // Arrange & Act
        // State: initial state holds the splash before an explicit session lookup
        val snapshot = MainSnapshot.from(viewModel)

        // Assert
        assertEquals(MainSnapshot(), snapshot)
        coVerify(exactly = 0) { getSessionUseCase() }
        verify(exactly = 0) { fcmTokenRegistrar.register() }
    }

    @Test
    fun `checkSession sets checking snapshot when initial lookup is pending`() = runTest(testDispatcher) {
        // Arrange
        val response = CompletableDeferred<AuthResult<AuthSession>>()
        coEvery { getSessionUseCase() } coAnswers { response.await() }

        // Act
        // Coroutine/State: an unfinished initial lookup keeps the splash without entering retry mode
        viewModel.checkSession()
        runCurrent()
        val pending = MainSnapshot.from(viewModel)
        response.complete(AuthResult.Success(AuthSession(state = AuthState.Guest)))
        runCurrent()

        // Assert
        assertEquals(MainSnapshot(), pending)
        assertEquals(MainSnapshot(MainUiState.Unauthenticated, false), MainSnapshot.from(viewModel))
        coVerify(exactly = 1) { getSessionUseCase() }
    }

    @Test
    fun `checkSession emits Authenticated when signed in`() = runTest(testDispatcher) {
        // Arrange
        coEvery { getSessionUseCase() } returns AuthResult.Success(AuthSession(state = AuthState.SignedIn))

        // Act & Assert
        // Flow/State: the public stream emits the destination and releases startup flags
        viewModel.uiState.test {
            assertEquals(MainUiState.None, awaitItem())
            viewModel.checkSession()
            runCurrent()
            assertEquals(MainUiState.Authenticated, awaitItem())
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
        assertEquals(MainSnapshot(MainUiState.Authenticated, false), MainSnapshot.from(viewModel))
        coVerify(exactly = 1) { getSessionUseCase() }
    }

    @Test
    fun `checkSession sets Unauthenticated when guest`() = runTest(testDispatcher) {
        // Arrange
        coEvery { getSessionUseCase() } returns AuthResult.Success(AuthSession(state = AuthState.Guest))

        // Act
        // State: guest selects login and releases the splash
        viewModel.checkSession()
        runCurrent()

        // Assert
        assertEquals(MainSnapshot(MainUiState.Unauthenticated, false), MainSnapshot.from(viewModel))
        coVerify(exactly = 1) { getSessionUseCase() }
    }

    @Test
    fun `checkSession sets SessionCheckError when lookup returns Error`() = runTest(testDispatcher) {
        // Arrange
        coEvery { getSessionUseCase() } returns AuthResult.Error(AuthError.Unknown())

        // Act
        // Error/State: a domain failure selects retry with the splash released
        viewModel.checkSession()
        runCurrent()

        // Assert
        assertEquals(MainSnapshot(MainUiState.SessionCheckError, false), MainSnapshot.from(viewModel))
        coVerify(exactly = 1) { getSessionUseCase() }
    }

    @Test
    fun `checkSession sets SessionCheckError when lookup throws`() = runTest(testDispatcher) {
        // Arrange
        coEvery { getSessionUseCase() } throws RuntimeException("network error")

        // Act
        // Error/State: an unexpected exception selects the same error state and releases flags
        viewModel.checkSession()
        runCurrent()

        // Assert
        assertEquals(MainSnapshot(MainUiState.SessionCheckError, false), MainSnapshot.from(viewModel))
        coVerify(exactly = 1) { getSessionUseCase() }
    }

    @Test
    fun `checkSession sets retry snapshot when retry lookup is pending`() = runTest(testDispatcher) {
        // Arrange
        failInitialCheck()
        val response = CompletableDeferred<AuthResult<AuthSession>>()
        coEvery { getSessionUseCase() } coAnswers { response.await() }

        // Act
        // Coroutine/State: retry keeps the error screen while progress is shown without restoring the splash
        viewModel.checkSession()
        runCurrent()
        val pending = MainSnapshot.from(viewModel)
        response.complete(AuthResult.Success(AuthSession(state = AuthState.SignedIn)))
        runCurrent()

        // Assert
        assertEquals(MainSnapshot(MainUiState.SessionCheckError, false, true), pending)
        assertEquals(MainSnapshot(MainUiState.Authenticated, false), MainSnapshot.from(viewModel))
        coVerify(exactly = 2) { getSessionUseCase() }
    }

    @Test
    fun `checkSession sets Unauthenticated when retry returns guest`() = runTest(testDispatcher) {
        // Arrange
        failInitialCheck()
        coEvery { getSessionUseCase() } returns AuthResult.Success(AuthSession(state = AuthState.Guest))

        // Act
        // State: a guest retry clears progress and selects the unauthenticated destination
        viewModel.checkSession()
        runCurrent()

        // Assert
        assertEquals(MainSnapshot(MainUiState.Unauthenticated, false), MainSnapshot.from(viewModel))
        coVerify(exactly = 2) { getSessionUseCase() }
    }

    @Test
    fun `checkSession sets retry flag to false when retry returns Error`() = runTest(testDispatcher) {
        // Arrange
        failInitialCheck()
        val response = CompletableDeferred<AuthResult<AuthSession>>()
        coEvery { getSessionUseCase() } coAnswers { response.await() }
        viewModel.checkSession()
        runCurrent()

        // Act
        // Error/State: another domain failure finishes retry and keeps the error destination
        response.complete(AuthResult.Error(AuthError.Unknown()))
        runCurrent()

        // Assert
        assertEquals(MainSnapshot(MainUiState.SessionCheckError, false), MainSnapshot.from(viewModel))
        coVerify(exactly = 2) { getSessionUseCase() }
    }

    @Test
    fun `checkSession sets retry flag to false when retry throws`() = runTest(testDispatcher) {
        // Arrange
        failInitialCheck()
        val response = CompletableDeferred<AuthResult<AuthSession>>()
        coEvery { getSessionUseCase() } coAnswers { response.await() }
        viewModel.checkSession()
        runCurrent()

        // Act
        // Error/State: an unexpected retry exception releases progress in finally
        response.completeExceptionally(RuntimeException("network error"))
        runCurrent()

        // Assert
        assertEquals(MainSnapshot(MainUiState.SessionCheckError, false), MainSnapshot.from(viewModel))
        coVerify(exactly = 2) { getSessionUseCase() }
    }

    @Test
    fun `checkSession ignores concurrent calls when initial lookup is pending`() = runTest(testDispatcher) {
        // Arrange
        val response = CompletableDeferred<AuthResult<AuthSession>>()
        coEvery { getSessionUseCase() } coAnswers { response.await() }

        // Act
        // Coroutine/Interaction: scheduled and already-running calls share one active lookup job
        viewModel.checkSession()
        viewModel.checkSession()
        runCurrent()
        viewModel.checkSession()
        val pending = MainSnapshot.from(viewModel)
        response.complete(AuthResult.Success(AuthSession(state = AuthState.SignedIn)))
        runCurrent()

        // Assert
        assertEquals(MainSnapshot(), pending)
        assertEquals(MainSnapshot(MainUiState.Authenticated, false), MainSnapshot.from(viewModel))
        coVerify(exactly = 1) { getSessionUseCase() }
    }

    @Test
    fun `checkSession ignores concurrent calls when retry is pending`() = runTest(testDispatcher) {
        // Arrange
        failInitialCheck()
        val response = CompletableDeferred<AuthResult<AuthSession>>()
        coEvery { getSessionUseCase() } coAnswers { response.await() }

        // Act
        // Coroutine/Interaction: repeated retry taps retain one request and its progress state
        viewModel.checkSession()
        runCurrent()
        viewModel.checkSession()
        val pending = MainSnapshot.from(viewModel)
        response.complete(AuthResult.Success(AuthSession(state = AuthState.Guest)))
        runCurrent()

        // Assert
        assertEquals(MainSnapshot(MainUiState.SessionCheckError, false, true), pending)
        assertEquals(MainSnapshot(MainUiState.Unauthenticated, false), MainSnapshot.from(viewModel))
        coVerify(exactly = 2) { getSessionUseCase() }
    }

    @Test
    fun `checkSession rethrows cancellation when initial lookup is cancelled`() = runTest(testDispatcher) {
        // Arrange
        val response = CompletableDeferred<AuthResult<AuthSession>>()
        lateinit var requestJob: Job
        coEvery { getSessionUseCase() } coAnswers {
            requestJob = currentCoroutineContext().job
            response.await()
        }
        viewModel.checkSession()
        runCurrent()

        // Act
        // Coroutine/State: dependency cancellation cancels the lookup and releases flags without an error state
        response.cancel(CancellationException("session lookup cancelled"))
        runCurrent()

        // Assert
        assertTrue(requestJob.isCancelled)
        assertEquals(MainSnapshot(isCheckingSession = false), MainSnapshot.from(viewModel))
        coVerify(exactly = 1) { getSessionUseCase() }
    }

    @Test
    fun `checkSession rethrows cancellation when retry is cancelled`() = runTest(testDispatcher) {
        // Arrange
        failInitialCheck()
        val response = CompletableDeferred<AuthResult<AuthSession>>()
        lateinit var requestJob: Job
        coEvery { getSessionUseCase() } coAnswers {
            requestJob = currentCoroutineContext().job
            response.await()
        }
        viewModel.checkSession()
        runCurrent()
        val pending = MainSnapshot.from(viewModel)

        // Act
        // Coroutine/State: a cancelled retry keeps the error screen and clears retry progress
        response.cancel(CancellationException("session retry cancelled"))
        runCurrent()

        // Assert
        assertEquals(MainSnapshot(MainUiState.SessionCheckError, false, true), pending)
        assertTrue(requestJob.isCancelled)
        assertEquals(MainSnapshot(MainUiState.SessionCheckError, false), MainSnapshot.from(viewModel))
        coVerify(exactly = 2) { getSessionUseCase() }
    }

    @Test
    fun `checkSession calls lookup again when previous lookup has completed`() = runTest(testDispatcher) {
        // Arrange
        coEvery { getSessionUseCase() } returns AuthResult.Success(AuthSession(state = AuthState.SignedIn))
        viewModel.checkSession()
        runCurrent()
        coEvery { getSessionUseCase() } returns AuthResult.Success(AuthSession(state = AuthState.Guest))

        // Act
        // Coroutine/State: the job guard permits a later check while the splash stays released
        viewModel.checkSession()
        runCurrent()

        // Assert
        assertEquals(MainSnapshot(MainUiState.Unauthenticated, false), MainSnapshot.from(viewModel))
        coVerify(exactly = 2) { getSessionUseCase() }
    }

    @Test
    fun `registerFcmToken calls registrar when explicitly requested`() {
        // Arrange
        val initial = MainSnapshot.from(viewModel)

        // Act
        // Interaction: registration delegates without changing startup state or requesting a session
        viewModel.registerFcmToken()

        // Assert
        verify(exactly = 1) { fcmTokenRegistrar.register() }
        coVerify(exactly = 0) { getSessionUseCase() }
        assertEquals(initial, MainSnapshot.from(viewModel))
    }

    @Test
    fun `checkSession ignores FCM registration when authentication completes`() = runTest(testDispatcher) {
        // Arrange
        coEvery { getSessionUseCase() } returns AuthResult.Success(AuthSession(state = AuthState.SignedIn))

        // Act
        // Interaction: authentication exposes state while the Activity owns the registration trigger
        viewModel.checkSession()
        runCurrent()

        // Assert
        assertEquals(MainSnapshot(MainUiState.Authenticated, false), MainSnapshot.from(viewModel))
        verify(exactly = 0) { fcmTokenRegistrar.register() }
    }

    private fun failInitialCheck() {
        coEvery { getSessionUseCase() } returns AuthResult.Error(AuthError.Unknown())
        viewModel.checkSession()
        testDispatcher.scheduler.runCurrent()
    }

    private data class MainSnapshot(
        val uiState: MainUiState = MainUiState.None,
        val isCheckingSession: Boolean = true,
        val isRetrying: Boolean = false
    ) {
        companion object {
            fun from(viewModel: MainViewModel): MainSnapshot = MainSnapshot(
                uiState = viewModel.uiState.value,
                isCheckingSession = viewModel.isCheckingSession.value,
                isRetrying = viewModel.isRetrying.value
            )
        }
    }
}
