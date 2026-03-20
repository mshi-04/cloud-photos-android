package com.appvoyager.cloudphotos.ui.media.uistate

import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount

data class MediaUiState(
    val screenState: ScreenState = ScreenState.None,
    val gridColumnCount: GridColumnCount = GridColumnCount.of(3),
    val isSettingsDialogVisible: Boolean = false
) {

    sealed class ScreenState {

        data object None : ScreenState()
        data object PermissionRequired : ScreenState()
        data class Success(val mediaList: List<Media>) : ScreenState()
        data class Error(val error: Throwable? = null) : ScreenState()

    }

}
