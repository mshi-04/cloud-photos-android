package com.appvoyager.cloudphotos.ui.media.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.domain.media.usecase.GetMediaListUseCase
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val getMediaListUseCase: GetMediaListUseCase,
    private val getGridColumnCountUseCase: GetGridColumnCountUseCase,
    private val setGridColumnCountUseCase: SetGridColumnCountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _effect = Channel<MediaEffect>(Channel.BUFFERED)
    val effect: Flow<MediaEffect> = _effect.receiveAsFlow()

    private var gridColumnJob: Job? = null
    private var mediaListJob: Job? = null

    fun onShowSettingsDialog() {
        _uiState.update { it.copy(isSettingsDialogVisible = true) }
    }

    fun onDismissSettingsDialog() {
        _uiState.update { it.copy(isSettingsDialogVisible = false) }
    }

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

    fun onRetry() {
        loadMediaList()
    }

    fun loadGridColumnCount() {
        gridColumnJob?.cancel()
        gridColumnJob = viewModelScope.launch {
            getGridColumnCountUseCase()
                .catch { _effect.send(MediaEffect.ShowSnackbar(R.string.error_unknown)) }
                .collect { gridColumnCount ->
                    _uiState.update { it.copy(gridColumnCount = gridColumnCount) }
                }
        }
    }

    fun loadMediaList() {
        mediaListJob?.cancel()
        mediaListJob = viewModelScope.launch {
            getMediaListUseCase()
                .map { it.getOrThrow() }
                .onStart {
                    _uiState.update { it.copy(isLoading = true, isError = false) }
                }
                .catch {
                    _uiState.update { it.copy(isLoading = false, isError = true) }
                    _effect.send(MediaEffect.ShowSnackbar(R.string.error_media_load_failed))
                }
                .collect { mediaList ->
                    _uiState.update {
                        it.copy(
                            mediaList = mediaList,
                            isLoading = false,
                            isError = false
                        )
                    }
                }
        }
    }
    
}
