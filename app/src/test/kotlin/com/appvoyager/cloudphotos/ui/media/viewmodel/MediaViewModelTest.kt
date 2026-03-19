package com.appvoyager.cloudphotos.ui.media.viewmodel

import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.usecase.GetMediaListUseCase
import com.appvoyager.cloudphotos.domain.media.usecase.PrepareUploadQueueUseCase
import com.appvoyager.cloudphotos.domain.media.usecase.ScheduleDeleteUseCase
import com.appvoyager.cloudphotos.domain.media.usecase.SyncUploadRecordsUseCase
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaCreatedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import com.appvoyager.cloudphotos.domain.settings.usecase.GetGridColumnCountUseCase
import com.appvoyager.cloudphotos.domain.settings.usecase.SetGridColumnCountUseCase
import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount
import com.appvoyager.cloudphotos.ui.media.effect.MediaEffect
import com.appvoyager.cloudphotos.ui.media.uistate.MediaUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
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

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getMediaListUseCase = mockk()
        getGridColumnCountUseCase = mockk()
        setGridColumnCountUseCase = mockk()
        syncUploadRecordsUseCase = mockk()
        prepareUploadQueueUseCase = mockk()
        scheduleDeleteUseCase = mockk()
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MediaViewModel {
        return MediaViewModel(
            getMediaListUseCase = getMediaListUseCase,
            getGridColumnCountUseCase = getGridColumnCountUseCase,
            setGridColumnCountUseCase = setGridColumnCountUseCase,
            syncUploadRecordsUseCase = syncUploadRecordsUseCase,
            prepareUploadQueueUseCase = prepareUploadQueueUseCase,
            scheduleDeleteUseCase = scheduleDeleteUseCase
        )
    }

    @Test
    fun `initial state has default values`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))

        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state.loadState is MediaUiState.LoadState.None)
    }

    @Test
    fun `loadMediaList success updates loadState to Success`() = runTest {
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
        val loadState = viewModel.uiState.value.loadState
        assertTrue(loadState is MediaUiState.LoadState.Success)
    }

    @Test
    fun `loadMediaList success contains expected media list`() = runTest {
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
        val loadState = viewModel.uiState.value.loadState
        assertEquals(expectedList, (loadState as MediaUiState.LoadState.Success).mediaList)
    }

    @Test
    fun `loadMediaList error sets loadState to Error`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        every { getMediaListUseCase() } returns flow { throw RuntimeException("load failed") }

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act
        viewModel.loadMediaList()
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.uiState.value.loadState is MediaUiState.LoadState.Error)
    }

    @Test
    fun `loadMediaList error sends snackbar effect`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        every { getMediaListUseCase() } returns flow { throw RuntimeException("load failed") }

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act
        viewModel.loadMediaList()
        advanceUntilIdle()

        // Assert
        val effect = viewModel.effect.first()
        assertEquals(
            R.string.error_media_load_failed,
            (effect as MediaEffect.ShowSnackbar).messageResId
        )
    }

    @Test
    fun `init collects grid column count from use case`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(5))

        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert
        assertEquals(GridColumnCount.of(5), viewModel.uiState.value.gridColumnCount)
    }

    @Test
    fun `grid column count flow error sends snackbar effect`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flow {
            emit(GridColumnCount.of(3))
            throw RuntimeException("error")
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert
        val effect = viewModel.effect.first()
        assertEquals(R.string.error_unknown, (effect as MediaEffect.ShowSnackbar).messageResId)
    }

    @Test
    fun `onGridColumnCountChanged calls setGridColumnCountUseCase`() = runTest {
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
    fun `onGridColumnCountChanged error sends snackbar effect`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { setGridColumnCountUseCase(any()) } throws RuntimeException("save failed")

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act
        viewModel.onGridColumnCountChanged(4)
        advanceUntilIdle()

        // Assert
        val effect = viewModel.effect.first()
        assertEquals(R.string.error_unknown, (effect as MediaEffect.ShowSnackbar).messageResId)
    }

    @Test
    fun `onShowSettingsDialog sets isSettingsDialogVisible to true`() = runTest {
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
    fun `onPermissionDenied sets loadState to PermissionRequired`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Act
        viewModel.onPermissionDenied()

        // Assert
        assertTrue(viewModel.uiState.value.loadState is MediaUiState.LoadState.PermissionRequired)
    }

    @Test
    fun `onDismissSettingsDialog sets isSettingsDialogVisible to false`() = runTest {
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
    fun `onScreenResumed calls syncUploadRecordsUseCase`() = runTest {
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
    fun `onScreenResumed calls prepareUploadQueueUseCase`() = runTest {
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
    fun `onScreenResumed calls scheduleDeleteUseCase`() = runTest {
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
    fun `onScreenResumed throttling limits syncUploadRecordsUseCase to one call`() = runTest {
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
    fun `onScreenResumed throttling limits prepareUploadQueueUseCase to one call`() = runTest {
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
    fun `onScreenResumed throttling limits scheduleDeleteUseCase to one call`() = runTest {
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
    fun `onScreenResumed syncRemote failure sends snackbar effect`() = runTest {
        // Arrange
        every { getGridColumnCountUseCase() } returns flowOf(GridColumnCount.of(3))
        coEvery { syncUploadRecordsUseCase() } throws RuntimeException("sync failed")
        coEvery { prepareUploadQueueUseCase() } just runs
        coEvery { scheduleDeleteUseCase() } just runs

        val viewModel = createViewModel()
        viewModel.elapsedRealtimeProvider = { MediaViewModel.MIN_RESUME_INTERVAL_MS }
        advanceUntilIdle()

        // Act
        viewModel.onScreenResumed()
        advanceUntilIdle()

        // Assert
        val effect = viewModel.effect.first()
        assertEquals(R.string.error_unknown, (effect as MediaEffect.ShowSnackbar).messageResId)
    }

}
