package com.appvoyager.cloudphotos.ui.media.viewmodel

import app.cash.turbine.test
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.usecase.DeleteUserUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.SignOutUseCase
import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.usecase.GetGridColumnCountUseCase
import com.appvoyager.cloudphotos.domain.media.usecase.GetMediaListUseCase
import com.appvoyager.cloudphotos.domain.media.usecase.PrepareUploadQueueUseCase
import com.appvoyager.cloudphotos.domain.media.usecase.ScheduleDeleteUseCase
import com.appvoyager.cloudphotos.domain.media.usecase.SetGridColumnCountUseCase
import com.appvoyager.cloudphotos.domain.media.usecase.SyncUploadRecordsUseCase
import com.appvoyager.cloudphotos.domain.media.valueobject.GridColumnCount
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaCreatedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import com.appvoyager.cloudphotos.ui.media.effect.MediaEffect
import com.appvoyager.cloudphotos.ui.media.effect.MediaSnackbarMessage
import com.appvoyager.cloudphotos.ui.media.uistate.MediaUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
class MediaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getMediaListUseCase: GetMediaListUseCase
    private lateinit var getGridColumnCountUseCase: GetGridColumnCountUseCase
    private lateinit var setGridColumnCountUseCase: SetGridColumnCountUseCase
    private lateinit var syncUploadRecordsUseCase: SyncUploadRecordsUseCase
    private lateinit var prepareUploadQueueUseCase: PrepareUploadQueueUseCase
    private lateinit var scheduleDeleteUseCase: ScheduleDeleteUseCase
    private lateinit var signOutUseCase: SignOutUseCase
    private lateinit var deleteUserUseCase: DeleteUserUseCase

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getMediaListUseCase = mockk()
        getGridColumnCountUseCase = mockk()
        setGridColumnCountUseCase = mockk()
        syncUploadRecordsUseCase = mockk()
        prepareUploadQueueUseCase = mockk()
        scheduleDeleteUseCase = mockk()
        signOutUseCase = mockk()
        deleteUserUseCase = mockk()
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MediaViewModel = MediaViewModel(
        getMediaListUseCase = getMediaListUseCase,
        getGridColumnCountUseCase = getGridColumnCountUseCase,
        setGridColumnCountUseCase = setGridColumnCountUseCase,
        syncUploadRecordsUseCase = syncUploadRecordsUseCase,
        prepareUploadQueueUseCase = prepareUploadQueueUseCase,
        scheduleDeleteUseCase = scheduleDeleteUseCase,
        signOutUseCase = signOutUseCase,
        deleteUserUseCase = deleteUserUseCase
    )

    @Test
    fun `init sets screenState to None when created`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))

        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state.screenState is MediaUiState.ScreenState.None)
    }

    @Test
    fun `loadMediaList sets screenState to Success when use case returns media list`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        val expectedList = listOf(
            Media(
                id = MediaId.of("1"),
                url = MediaUrl.of("content://media/external/images/1"),
                type = MediaType.IMAGE,
                createdAt = MediaCreatedAt.of(1700000000000L)
            )
        )
        every { getMediaListUseCase() } returns flowOf(expectedList)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act
        viewModel.loadMediaList()
        advanceUntilIdle()

        // Assert
        val screenState = viewModel.uiState.value.screenState
        assertTrue(screenState is MediaUiState.ScreenState.Success)
    }

    @Test
    fun `loadMediaList sets mediaList when use case returns media list`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        val expectedList = listOf(
            Media(
                id = MediaId.of("1"),
                url = MediaUrl.of("content://media/external/images/1"),
                type = MediaType.IMAGE,
                createdAt = MediaCreatedAt.of(1700000000000L)
            )
        )
        every { getMediaListUseCase() } returns flowOf(expectedList)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act
        viewModel.loadMediaList()
        advanceUntilIdle()

        // Assert
        val screenState = viewModel.uiState.value.screenState
        assertEquals(expectedList, (screenState as MediaUiState.ScreenState.Success).mediaList)
    }

    @Test
    fun `loadMediaList sets screenState to Error when use case throws`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        every { getMediaListUseCase() } returns flow { throw RuntimeException("load failed") }

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act
        viewModel.loadMediaList()
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.uiState.value.screenState is MediaUiState.ScreenState.Error)
    }

    @Test
    fun `loadMediaList emits ShowSnackbar when use case throws`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        every { getMediaListUseCase() } returns flow { throw RuntimeException("load failed") }

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act & Assert
        // Flow: media load failure emits snackbar effect
        viewModel.effect.test {
            viewModel.loadMediaList()
            advanceUntilIdle()

            assertEquals(
                MediaSnackbarMessage.MediaLoadFailed,
                (awaitItem() as MediaEffect.ShowSnackbar).message
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `init sets gridColumnCount when use case emits value`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(5))

        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert
        assertEquals(GridColumnCount.of(5), viewModel.uiState.value.gridColumnCount)
    }

    @Test
    fun `init emits ShowSnackbar when grid column count flow throws`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flow {
            emit(GridColumnCount.of(3))
            throw RuntimeException("error")
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act & Assert
        // Flow: grid column flow failure emits unknown snackbar
        viewModel.effect.test {
            assertEquals(MediaSnackbarMessage.Unknown, (awaitItem() as MediaEffect.ShowSnackbar).message)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onGridColumnCountChanged calls setGridColumnCountUseCase when called with new count`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { setGridColumnCountUseCase(any()) } returns Unit

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act
        viewModel.onGridColumnCountChanged(4)
        advanceUntilIdle()

        // Assert
        coVerify { setGridColumnCountUseCase(GridColumnCount.of(4)) }
    }

    @Test
    fun `onGridColumnCountChanged emits ShowSnackbar when use case throws`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { setGridColumnCountUseCase(any()) } throws RuntimeException("save failed")

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act & Assert
        // Flow: grid column save failure emits unknown snackbar
        viewModel.effect.test {
            viewModel.onGridColumnCountChanged(4)
            advanceUntilIdle()

            assertEquals(MediaSnackbarMessage.Unknown, (awaitItem() as MediaEffect.ShowSnackbar).message)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onShowSettingsDialog sets isSettingsDialogVisible to true when called`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act
        viewModel.onShowSettingsDialog()

        // Assert
        assertTrue(viewModel.uiState.value.isSettingsDialogVisible)
    }

    @Test
    fun `onPermissionDenied sets screenState to PermissionRequired when called`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act
        viewModel.onPermissionDenied()

        // Assert
        assertTrue(viewModel.uiState.value.screenState is MediaUiState.ScreenState.PermissionRequired)
    }

    @Test
    fun `onDismissSettingsDialog sets isSettingsDialogVisible to false when called`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onShowSettingsDialog()

        // Act
        viewModel.onDismissSettingsDialog()

        // Assert
        assertFalse(viewModel.uiState.value.isSettingsDialogVisible)
    }

    @Test
    fun `onScreenResumed calls syncUploadRecordsUseCase when interval has elapsed`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { syncUploadRecordsUseCase() } returns Unit
        coEvery { prepareUploadQueueUseCase() } just runs
        coEvery { scheduleDeleteUseCase() } just runs

        val viewModel = createViewModel()
        viewModel.elapsedRealtimeProvider = { MediaViewModel.MIN_RESUME_INTERVAL_MS }
        advanceUntilIdle()

        // Act
        viewModel.onScreenResumed()
        advanceUntilIdle()

        // Assert
        coVerify { syncUploadRecordsUseCase() }
    }

    @Test
    fun `onScreenResumed calls prepareUploadQueueUseCase when interval has elapsed`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { syncUploadRecordsUseCase() } returns Unit
        coEvery { prepareUploadQueueUseCase() } just runs
        coEvery { scheduleDeleteUseCase() } just runs

        val viewModel = createViewModel()
        viewModel.elapsedRealtimeProvider = { MediaViewModel.MIN_RESUME_INTERVAL_MS }
        advanceUntilIdle()

        // Act
        viewModel.onScreenResumed()
        advanceUntilIdle()

        // Assert
        coVerify { prepareUploadQueueUseCase() }
    }

    @Test
    fun `onScreenResumed calls scheduleDeleteUseCase when interval has elapsed`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { syncUploadRecordsUseCase() } returns Unit
        coEvery { prepareUploadQueueUseCase() } just runs
        coEvery { scheduleDeleteUseCase() } just runs

        val viewModel = createViewModel()
        viewModel.elapsedRealtimeProvider = { MediaViewModel.MIN_RESUME_INTERVAL_MS }
        advanceUntilIdle()

        // Act
        viewModel.onScreenResumed()
        advanceUntilIdle()

        // Assert
        coVerify { scheduleDeleteUseCase() }
    }

    @Test
    fun `onScreenResumed ignores syncUploadRecordsUseCase when interval has not elapsed`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { syncUploadRecordsUseCase() } returns Unit
        coEvery { prepareUploadQueueUseCase() } just runs
        coEvery { scheduleDeleteUseCase() } just runs

        val viewModel = createViewModel()
        viewModel.elapsedRealtimeProvider = { MediaViewModel.MIN_RESUME_INTERVAL_MS }
        advanceUntilIdle()

        // Act: 1回目は通過、2回目は同じ時刻なのでスロットルされる
        viewModel.onScreenResumed()
        advanceUntilIdle()
        viewModel.onScreenResumed()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { syncUploadRecordsUseCase() }
    }

    @Test
    fun `onScreenResumed ignores prepareUploadQueueUseCase when interval has not elapsed`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { syncUploadRecordsUseCase() } returns Unit
        coEvery { prepareUploadQueueUseCase() } just runs
        coEvery { scheduleDeleteUseCase() } just runs

        val viewModel = createViewModel()
        viewModel.elapsedRealtimeProvider = { MediaViewModel.MIN_RESUME_INTERVAL_MS }
        advanceUntilIdle()

        // Act: 1回目は通過、2回目は同じ時刻なのでスロットルされる
        viewModel.onScreenResumed()
        advanceUntilIdle()
        viewModel.onScreenResumed()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { prepareUploadQueueUseCase() }
    }

    @Test
    fun `onScreenResumed ignores scheduleDeleteUseCase when interval has not elapsed`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { syncUploadRecordsUseCase() } returns Unit
        coEvery { prepareUploadQueueUseCase() } just runs
        coEvery { scheduleDeleteUseCase() } just runs

        val viewModel = createViewModel()
        viewModel.elapsedRealtimeProvider = { MediaViewModel.MIN_RESUME_INTERVAL_MS }
        advanceUntilIdle()

        // Act: 1回目は通過、2回目は同じ時刻なのでスロットルされる
        viewModel.onScreenResumed()
        advanceUntilIdle()
        viewModel.onScreenResumed()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { scheduleDeleteUseCase() }
    }

    @Test
    fun `onScreenResumed emits ShowSnackbar when syncUploadRecordsUseCase throws`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { syncUploadRecordsUseCase() } throws RuntimeException("sync failed")
        coEvery { prepareUploadQueueUseCase() } just runs
        coEvery { scheduleDeleteUseCase() } just runs

        val viewModel = createViewModel()
        viewModel.elapsedRealtimeProvider = { MediaViewModel.MIN_RESUME_INTERVAL_MS }
        advanceUntilIdle()

        // Act & Assert
        // Flow: sync failure emits unknown snackbar
        viewModel.effect.test {
            viewModel.onScreenResumed()
            advanceUntilIdle()

            assertEquals(MediaSnackbarMessage.Unknown, (awaitItem() as MediaEffect.ShowSnackbar).message)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `signOut emits NavigateToLogin when sign out succeeds`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { signOutUseCase() } returns AuthResult.Success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act & Assert
        // Flow: successful sign out emits login navigation effect
        viewModel.effect.test {
            viewModel.signOut()
            advanceUntilIdle()

            assertEquals(MediaEffect.NavigateToLogin, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `signOut emits ShowSnackbar when sign out returns error`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { signOutUseCase() } returns AuthResult.Error(AuthError.Unknown())

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act & Assert
        // Flow: sign out domain error emits failure snackbar
        viewModel.effect.test {
            viewModel.signOut()
            advanceUntilIdle()

            assertEquals(MediaSnackbarMessage.SignOutFailed, (awaitItem() as MediaEffect.ShowSnackbar).message)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `signOut emits ShowSnackbar when sign out throws`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { signOutUseCase() } throws RuntimeException("sign out failed")

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act & Assert
        // Flow: sign out exception emits failure snackbar
        viewModel.effect.test {
            viewModel.signOut()
            advanceUntilIdle()

            assertEquals(MediaSnackbarMessage.SignOutFailed, (awaitItem() as MediaEffect.ShowSnackbar).message)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `signOut sets isSigningOut to false when completed`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { signOutUseCase() } returns AuthResult.Success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act
        viewModel.signOut()
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.uiState.value.isSigningOut)
    }

    @Test
    fun `signOut ignores concurrent call when already signing out`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        val deferred = kotlinx.coroutines.CompletableDeferred<AuthResult<Unit>>()
        coEvery { signOutUseCase() } coAnswers { deferred.await() }

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act: 1回目を開始してisSigningOut=trueになった後、2回目を呼ぶ
        viewModel.signOut()
        testScheduler.advanceTimeBy(1)
        viewModel.signOut()

        deferred.complete(AuthResult.Success(Unit))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { signOutUseCase() }
    }

    @Test
    fun `deleteUser emits NavigateAfterAccountDeletion when delete user succeeds`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { deleteUserUseCase() } returns AuthResult.Success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act & Assert
        // Flow: successful account deletion emits deletion navigation effect
        viewModel.effect.test {
            viewModel.deleteUser()
            advanceUntilIdle()

            assertEquals(MediaEffect.NavigateAfterAccountDeletion, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deleteUser emits ShowSnackbar when delete user returns error`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { deleteUserUseCase() } returns AuthResult.Error(AuthError.Unknown())

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act & Assert
        // Flow: account deletion domain error emits failure snackbar
        viewModel.effect.test {
            viewModel.deleteUser()
            advanceUntilIdle()

            assertEquals(MediaSnackbarMessage.DeleteUserFailed, (awaitItem() as MediaEffect.ShowSnackbar).message)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deleteUser emits ShowSnackbar when delete user throws`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { deleteUserUseCase() } throws RuntimeException("delete user failed")

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act & Assert
        // Flow: account deletion exception emits failure snackbar
        viewModel.effect.test {
            viewModel.deleteUser()
            advanceUntilIdle()

            assertEquals(MediaSnackbarMessage.DeleteUserFailed, (awaitItem() as MediaEffect.ShowSnackbar).message)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deleteUser ignores concurrent call when already deleting user`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        val deferred = kotlinx.coroutines.CompletableDeferred<AuthResult<Unit>>()
        coEvery { deleteUserUseCase() } coAnswers { deferred.await() }

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act: 1回目を開始してisDeletingUser=trueになった後、2回目を呼ぶ
        viewModel.deleteUser()
        testScheduler.advanceTimeBy(1)
        viewModel.deleteUser()

        deferred.complete(AuthResult.Success(Unit))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { deleteUserUseCase() }
    }
}
