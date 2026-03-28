package com.appvoyager.cloudphotos.ui.media.effect

import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl

sealed class CameraEffect {
    data object ShowStorageFullDialog : CameraEffect()
    data class ShowSnackbar(val message: CameraSnackbarMessage) : CameraEffect()
    data class OnPhotoCaptured(val mediaUrl: MediaUrl) : CameraEffect()
}
