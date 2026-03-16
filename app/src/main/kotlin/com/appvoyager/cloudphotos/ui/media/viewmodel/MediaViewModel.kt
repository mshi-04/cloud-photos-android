package com.appvoyager.cloudphotos.ui.media.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.domain.media.usecase.GetMediaListUseCase
import com.appvoyager.cloudphotos.domain.media.usecase.ScheduleDeleteUseCase
import com.appvoyager.cloudphotos.domain.media.usecase.ScheduleUploadUseCase
import com.appvoyager.cloudphotos.domain.media.usecase.SyncUploadRecordsUseCase
import com.appvoyager.cloudphotos.domain.settings.usecase.GetGridColumnCountUseCase
import com.appvoyager.cloudphotos.domain.settings.usecase.SetGridColumnCountUseCase
import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount
import com.appvoyager.cloudphotos.ui.media.effect.MediaEffect
import com.appvoyager.cloudphotos.ui.media.uistate.MediaUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val scheduleUploadUseCase: ScheduleUploadUseCase,
    private val scheduleDeleteUseCase: ScheduleDeleteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _effect = Channel<MediaEffect>(Channel.BUFFERED)
    val effect: Flow<MediaEffect> = _effect.receiveAsFlow()

    private var mediaListJob: Job? = null
    private var syncJob: Job? = null
    private var lastResumeTimeMs = 0L

    init {
        viewModelScope.launch {
            getGridColumnCountUseCase()
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    _effect.send(MediaEffect.ShowSnackbar(R.string.error_unknown))
                }
                .collect { gridColumnCount ->
                    _uiState.update { it.copy(gridColumnCount = gridColumnCount) }
                }
        }
    }

    fun onShowSettingsDialog() =
        _uiState.update { it.copy(isSettingsDialogVisible = true) }

    fun onDismissSettingsDialog() =
        _uiState.update { it.copy(isSettingsDialogVisible = false) }

    fun onGridColumnCountChanged(count: Int) {
        viewModelScope.launch {
            try {
                val gridColumnCount = GridColumnCount.of(count)
                setGridColumnCountUseCase(gridColumnCount)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _effect.send(MediaEffect.ShowSnackbar(R.string.error_unknown))
            }
        }
    }

    fun onPermissionDenied() =
        _uiState.update { it.copy(loadState = MediaUiState.LoadState.PermissionRequired) }

    fun loadMediaList() {
        mediaListJob?.cancel()
        _uiState.update { it.copy(loadState = MediaUiState.LoadState.Loading) }
        mediaListJob = viewModelScope.launch {
            getMediaListUseCase()
                .catch { cause ->
                    if (cause is SecurityException) {
                        _uiState.update { it.copy(loadState = MediaUiState.LoadState.PermissionRequired) }
                    } else {
                        _uiState.update { it.copy(loadState = MediaUiState.LoadState.Error(cause)) }
                        _effect.send(MediaEffect.ShowSnackbar(R.string.error_media_load_failed))
                    }
                }
                .collect { mediaList ->
                    _uiState.update {
                        it.copy(loadState = MediaUiState.LoadState.Success(mediaList))
                    }
                }
        }
    }

    fun onResume() {
        val now = System.currentTimeMillis()
        if (now - lastResumeTimeMs < MIN_RESUME_INTERVAL_MS) return
        lastResumeTimeMs = now
        syncRemote()
        scheduleUpload()
        scheduleDelete()
    }

    fun syncRemote() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            runCatching { syncUploadRecordsUseCase() }
                .onFailure {
                    if (it is CancellationException) throw it
                    _effect.send(MediaEffect.ShowSnackbar(R.string.error_unknown))
                }
        }
    }

    fun scheduleUpload() = viewModelScope.launch {
        runCatching { scheduleUploadUseCase() }
            .onFailure { if (it is CancellationException) throw it }
    }

    fun scheduleDelete() = viewModelScope.launch {
        runCatching { scheduleDeleteUseCase() }
            .onFailure { if (it is CancellationException) throw it }
    }

    companion object {
        private const val MIN_RESUME_INTERVAL_MS = 3_000L
    }

}
