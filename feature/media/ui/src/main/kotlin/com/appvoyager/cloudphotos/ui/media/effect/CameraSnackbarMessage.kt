package com.appvoyager.cloudphotos.ui.media.effect

sealed class CameraSnackbarMessage {
    data object CameraUnavailable : CameraSnackbarMessage()
    data object CaptureFailed : CameraSnackbarMessage()
}
