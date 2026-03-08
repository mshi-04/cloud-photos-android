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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val getMediaListUseCase: GetMediaListUseCase,
    private val getGridColumnCountUseCase: GetGridColumnCountUseCase,
    private val setGridColumnCountUseCase: SetGridColumnCountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<MediaEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<MediaEffect> = _effect.asSharedFlow()

    fun onShowSettingsDialog() {
        _uiState.update { it.copy(isSettingsDialogVisible = true) }
    }

    fun onDismissSettingsDialog() {
        _uiState.update { it.copy(isSettingsDialogVisible = false) }
    }

    fun onGridColumnCountChanged(count: Int) {
        viewModelScope.launch {
            val gridColumnCount = GridColumnCount.of(count)
            setGridColumnCountUseCase(gridColumnCount)
        }
    }

    fun onRetry() {
        loadMediaList()
    }

    fun loadGridColumnCount() {
        viewModelScope.launch {
            getGridColumnCountUseCase().collect { gridColumnCount ->
                _uiState.update { it.copy(gridColumnCount = gridColumnCount) }
            }
        }
    }

    fun loadMediaList() {
        _uiState.update { it.copy(isLoading = true, isError = false) }
        viewModelScope.launch {
            getMediaListUseCase().collect { result ->
                result.fold(
                    onSuccess = { mediaList ->
                        _uiState.update {
                            it.copy(
                                mediaList = mediaList,
                                isLoading = false,
                                isError = false
                            )
                        }
                    },
                    onFailure = {
                        _uiState.update { it.copy(isLoading = false, isError = true) }
                        _effect.emit(MediaEffect.ShowSnackbar(R.string.error_media_load_failed))
                    }
                )
            }
        }
    }
    
}
