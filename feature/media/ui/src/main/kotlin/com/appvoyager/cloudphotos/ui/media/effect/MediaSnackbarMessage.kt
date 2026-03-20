package com.appvoyager.cloudphotos.ui.media.effect

sealed class MediaSnackbarMessage {
    data object Unknown : MediaSnackbarMessage()
    data object MediaLoadFailed : MediaSnackbarMessage()
}
