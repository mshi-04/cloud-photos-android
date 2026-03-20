package com.appvoyager.cloudphotos.ui.media.effect

sealed class MediaEffect {
    data class ShowSnackbar(val message: MediaSnackbarMessage) : MediaEffect()
}
