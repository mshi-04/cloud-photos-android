package com.appvoyager.cloudphotos.ui.media.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.domain.media.usecase.GetMediaListUseCase
import com.appvoyager.cloudphotos.ui.media.effect.MediaDetailEffect
import com.appvoyager.cloudphotos.ui.media.uistate.MediaDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
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
class MediaDetailViewModel @Inject constructor(
    private val getMediaListUseCase: GetMediaListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaDetailUiState())
    val uiState: StateFlow<MediaDetailUiState> = _uiState.asStateFlow()

    private val _effect = Channel<MediaDetailEffect>(Channel.BUFFERED)
    val effect: Flow<MediaDetailEffect> = _effect.receiveAsFlow()

    init {
        loadMediaList()
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            _effect.send(MediaDetailEffect.NavigateBack)
        }
    }

    private fun loadMediaList() {
        viewModelScope.launch {
            getMediaListUseCase()
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    _uiState.update { it.copy(screenState = MediaDetailUiState.ScreenState.Error(cause)) }
                }
                .collect { mediaList ->
                    _uiState.update {
                        it.copy(screenState = MediaDetailUiState.ScreenState.Success(mediaList))
                    }
                }
        }
    }

}
