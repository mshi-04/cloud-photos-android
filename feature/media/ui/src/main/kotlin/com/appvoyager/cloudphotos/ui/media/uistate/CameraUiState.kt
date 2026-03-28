package com.appvoyager.cloudphotos.ui.media.uistate

sealed class CameraUiState {
    data object CheckingPermission : CameraUiState()
    data object PermissionRequired : CameraUiState()
    data object Ready : CameraUiState()
    data object Capturing : CameraUiState()
    data class Error(val type: ErrorType) : CameraUiState()

    enum class ErrorType {
        STORAGE_FULL,
        CAMERA_UNAVAILABLE,
        CAPTURE_FAILED
    }
}
