package com.appvoyager.cloudphotos.ui.media.viewmodel

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.domain.media.model.PhotoCaptureHandle
import com.appvoyager.cloudphotos.domain.media.model.SavePhotoResult
import com.appvoyager.cloudphotos.ui.media.effect.CameraEffect
import com.appvoyager.cloudphotos.ui.media.effect.CameraSnackbarMessage
import com.appvoyager.cloudphotos.ui.media.uistate.CameraUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.CheckingPermission)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _effect = Channel<CameraEffect>(Channel.BUFFERED)
    val effect: Flow<CameraEffect> = _effect.receiveAsFlow()

    fun onPermissionGranted() {
        val current = _uiState.value
        if (current is CameraUiState.CheckingPermission || current is CameraUiState.PermissionRequired) {
            _uiState.update { CameraUiState.Ready }
        }
    }

    fun onPermissionDenied() {
        _uiState.update { CameraUiState.PermissionRequired }
    }

    fun onCameraError() {
        viewModelScope.launch {
            _uiState.update { CameraUiState.Error(CameraUiState.ErrorType.CAMERA_UNAVAILABLE) }
            _effect.send(CameraEffect.ShowSnackbar(CameraSnackbarMessage.CameraUnavailable))
        }
    }

    fun retryCamera() {
        _uiState.update { CameraUiState.Ready }
    }

    fun takePhoto(handle: PhotoCaptureHandle, onCaptureAnimTrigger: () -> Unit) {
        if (_uiState.value !is CameraUiState.Ready) return
        _uiState.update { CameraUiState.Capturing }

        onCaptureAnimTrigger()

        viewModelScope.launch {
            try {
                when (val result = handle.capture()) {
                    is SavePhotoResult.Success -> {
                        _uiState.update { CameraUiState.Ready }
                        _effect.send(CameraEffect.OnPhotoCaptured(result.url.value.toUri()))
                    }

                    is SavePhotoResult.Error -> handleSaveError(result.type)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                handleSaveError(SavePhotoResult.ErrorType.SAVE_FAILED)
            }
        }
    }

    private suspend fun handleSaveError(type: SavePhotoResult.ErrorType) {
        val errorType = when (type) {
            SavePhotoResult.ErrorType.STORAGE_FULL -> CameraUiState.ErrorType.STORAGE_FULL
            SavePhotoResult.ErrorType.SAVE_FAILED -> CameraUiState.ErrorType.CAPTURE_FAILED
        }
        _uiState.update { CameraUiState.Error(errorType) }
        if (errorType == CameraUiState.ErrorType.STORAGE_FULL) {
            _effect.send(CameraEffect.ShowStorageFullDialog)
        } else {
            _effect.send(CameraEffect.ShowSnackbar(CameraSnackbarMessage.CaptureFailed))
        }
    }

}
