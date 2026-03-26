package com.appvoyager.cloudphotos.ui.media.effect

import android.net.Uri

sealed class CameraEffect {
    data object ShowStorageFullDialog : CameraEffect()
    data class ShowSnackbar(val message: CameraSnackbarMessage) : CameraEffect()
    data class OnPhotoCaptured(val thumbnailUri: Uri) : CameraEffect()
}
