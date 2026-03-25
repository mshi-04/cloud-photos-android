package com.appvoyager.cloudphotos.ui.media.uistate

import com.appvoyager.cloudphotos.domain.media.model.Media

data class MediaDetailUiState(
    val screenState: ScreenState = ScreenState.Loading
) {
    sealed class ScreenState {
        data object Loading : ScreenState()
        data class Success(val mediaList: List<Media>) : ScreenState()
        data class Error(val error: Throwable? = null) : ScreenState()
    }
}
