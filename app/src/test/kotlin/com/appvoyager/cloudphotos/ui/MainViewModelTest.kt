package com.appvoyager.cloudphotos.ui

import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.model.AuthState
import com.appvoyager.cloudphotos.domain.auth.usecase.GetSessionUseCase
import com.appvoyager.cloudphotos.fcm.FcmTokenRegistrar
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
        Dispatchers.setMain(testDispatcher)
        every { fcmTokenRegistrar.register() } just runs
        viewModel = MainViewModel(getSessionUseCase, fcmTokenRegistrar)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is None`() {
        assertEquals(MainUiState.None, viewModel.uiState)
    }

    @Test
    fun `initial isCheckingSession is true`() {
        assertTrue(viewModel.isCheckingSession)
    }

    @Test
    fun `checkSession sets Authenticated when signed in`() = runTest {
        coEvery { getSessionUseCase() } returns AuthResult.Success(
            AuthSession(state = AuthState.SignedIn)
        )

        viewModel.checkSession()
        advanceUntilIdle()

        assertEquals(MainUiState.Authenticated, viewModel.uiState)
    }

    @Test
    fun `checkSession sets Unauthenticated when guest`() = runTest {
        coEvery { getSessionUseCase() } returns AuthResult.Success(
            AuthSession(state = AuthState.Guest)
        )

        viewModel.checkSession()
        advanceUntilIdle()

        assertEquals(MainUiState.Unauthenticated, viewModel.uiState)
    }

    @Test
    fun `checkSession sets SessionCheckError on AuthResult Error`() = runTest {
        coEvery { getSessionUseCase() } returns AuthResult.Error(AuthError.Unknown())

        viewModel.checkSession()
        advanceUntilIdle()

        assertEquals(MainUiState.SessionCheckError, viewModel.uiState)
    }

    @Test
    fun `checkSession sets SessionCheckError on exception`() = runTest {
        coEvery { getSessionUseCase() } throws RuntimeException("network error")

        viewModel.checkSession()
        advanceUntilIdle()

        assertEquals(MainUiState.SessionCheckError, viewModel.uiState)
    }

    @Test
    fun `checkSession sets isCheckingSession to false after completion`() = runTest {
        coEvery { getSessionUseCase() } returns AuthResult.Success(
            AuthSession(state = AuthState.SignedIn)
        )

        viewModel.checkSession()
        advanceUntilIdle()

        assertFalse(viewModel.isCheckingSession)
    }

    @Test
    fun `checkSession does not run concurrently`() = runTest {
        coEvery { getSessionUseCase() } returns AuthResult.Success(
            AuthSession(state = AuthState.SignedIn)
        )

        viewModel.checkSession()
        viewModel.checkSession()
        advanceUntilIdle()

        // Only one call should have been made
        io.mockk.coVerify(exactly = 1) { getSessionUseCase() }
    }

    @Test
    fun `checkSession sets isRetrying to true while retrying from error`() = runTest {
        // Arrange
        coEvery { getSessionUseCase() } returns AuthResult.Error(AuthError.Unknown())

        viewModel.checkSession()
        advanceUntilIdle()

        assertEquals(MainUiState.SessionCheckError, viewModel.uiState)

        val deferred = kotlinx.coroutines.CompletableDeferred<AuthResult<AuthSession>>()
        coEvery { getSessionUseCase() } coAnswers { deferred.await() }

        // Act
        viewModel.checkSession()
        testScheduler.advanceTimeBy(1)

        // Assert
        assertTrue(viewModel.isRetrying)

        deferred.complete(AuthResult.Success(AuthSession(state = AuthState.SignedIn)))
        advanceUntilIdle()
    }

    @Test
    fun `checkSession sets isRetrying to false after retry completes`() = runTest {
        // Arrange
        coEvery { getSessionUseCase() } returns AuthResult.Error(AuthError.Unknown())

        viewModel.checkSession()
        advanceUntilIdle()

        assertEquals(MainUiState.SessionCheckError, viewModel.uiState)

        val deferred = kotlinx.coroutines.CompletableDeferred<AuthResult<AuthSession>>()
        coEvery { getSessionUseCase() } coAnswers { deferred.await() }

        // Act
        viewModel.checkSession()
        testScheduler.advanceTimeBy(1)
        deferred.complete(AuthResult.Success(AuthSession(state = AuthState.SignedIn)))
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.isRetrying)
    }

    @Test
    fun `registerFcmToken delegates to FcmTokenRegistrar`() {
        viewModel.registerFcmToken()

        verify(exactly = 1) { fcmTokenRegistrar.register() }
    }
}
