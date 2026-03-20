package com.appvoyager.cloudphotos.ui.media.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.domain.media.usecase.GetMediaListUseCase
import com.appvoyager.cloudphotos.domain.media.usecase.PrepareUploadQueueUseCase
import com.appvoyager.cloudphotos.domain.media.usecase.ScheduleDeleteUseCase
import com.appvoyager.cloudphotos.domain.media.usecase.SyncUploadRecordsUseCase
import com.appvoyager.cloudphotos.domain.settings.usecase.GetGridColumnCountUseCase
import com.appvoyager.cloudphotos.domain.settings.usecase.SetGridColumnCountUseCase
import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount
import com.appvoyager.cloudphotos.ui.media.effect.MediaEffect
import com.appvoyager.cloudphotos.ui.media.effect.MediaSnackbarMessage
import com.appvoyager.cloudphotos.ui.media.uistate.MediaUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val getMediaListUseCase: GetMediaListUseCase,
    private val getGridColumnCountUseCase: GetGridColumnCountUseCase,
    private val setGridColumnCountUseCase: SetGridColumnCountUseCase,
    private val syncUploadRecordsUseCase: SyncUploadRecordsUseCase,
    private val prepareUploadQueueUseCase: PrepareUploadQueueUseCase,
    private val scheduleDeleteUseCase: ScheduleDeleteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _effect = Channel<MediaEffect>(Channel.BUFFERED)
    val effect: Flow<MediaEffect> = _effect.receiveAsFlow()

    private var mediaListJob: Job? = null
    private var syncJob: Job? = null
    private var lastResumeElapsedRealtimeMs: Long? = null
    internal var elapsedRealtimeProvider: () -> Long = { SystemClock.elapsedRealtime() }

    init {
        viewModelScope.launch {
            getGridColumnCountUseCase()
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    _effect.send(MediaEffect.ShowSnackbar(MediaSnackbarMessage.Unknown))
                }
                .collect { gridColumnCount ->
                    _uiState.update { it.copy(gridColumnCount = gridColumnCount) }
                }
        }
    }

    fun onScreenResumed() {
        val now = elapsedRealtimeProvider()
        val last = lastResumeElapsedRealtimeMs
        if (last != null && now - last < MIN_RESUME_INTERVAL_MS) return
        lastResumeElapsedRealtimeMs = now
        val previousJob = syncJob
        syncJob = viewModelScope.launch {
            previousJob?.cancelAndJoin()
            syncRemote()
            prepareUploadQueue()
            scheduleDelete()
        }
    }

    fun loadMediaList() {
        mediaListJob?.cancel()
        _uiState.update { it.copy(screenState = MediaUiState.ScreenState.None) }
        mediaListJob = viewModelScope.launch {
            getMediaListUseCase()
                .catch { cause ->
                    if (cause is SecurityException) {
                        _uiState.update { it.copy(screenState = MediaUiState.ScreenState.PermissionRequired) }
                    } else {
                        _uiState.update { it.copy(screenState = MediaUiState.ScreenState.Error(cause)) }
                        _effect.send(MediaEffect.ShowSnackbar(MediaSnackbarMessage.MediaLoadFailed))
                    }
                }
                .collect { mediaList ->
                    _uiState.update {
                        it.copy(screenState = MediaUiState.ScreenState.Success(mediaList))
                    }
                }
        }
    }

    fun onGridColumnCountChanged(count: Int) {
        viewModelScope.launch {
            try {
                val gridColumnCount = GridColumnCount.of(count)
                setGridColumnCountUseCase(gridColumnCount)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _effect.send(MediaEffect.ShowSnackbar(MediaSnackbarMessage.Unknown))
            }
        }
    }

    fun onShowSettingsDialog() =
        _uiState.update { it.copy(isSettingsDialogVisible = true) }

    fun onDismissSettingsDialog() =
        _uiState.update { it.copy(isSettingsDialogVisible = false) }

    fun onPermissionDenied() =
        _uiState.update { it.copy(screenState = MediaUiState.ScreenState.PermissionRequired) }

    private suspend fun syncRemote() {
        runCatching { syncUploadRecordsUseCase() }
            .onFailure { cause ->
                if (cause is CancellationException) throw cause
                _effect.send(MediaEffect.ShowSnackbar(MediaSnackbarMessage.Unknown))
            }
    }

    private suspend fun prepareUploadQueue() {
        runCatching { prepareUploadQueueUseCase() }
            .onFailure { if (it is CancellationException) throw it }
    }

    private suspend fun scheduleDelete() {
        runCatching { scheduleDeleteUseCase() }
            .onFailure { if (it is CancellationException) throw it }
    }

    companion object {
        internal const val MIN_RESUME_INTERVAL_MS = 3_000L
    }

}
