package com.appvoyager.cloudphotos.ui.media.effect

sealed class MediaSnackbarMessage {
    data object Unknown : MediaSnackbarMessage()
    data object MediaLoadFailed : MediaSnackbarMessage()
    data object SignOutFailed : MediaSnackbarMessage()
    data object DeleteUserFailed : MediaSnackbarMessage()
}
