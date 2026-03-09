package com.appvoyager.cloudphotos.ui.media.uistate

import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount

data class MediaUiState(
    val loadState: LoadState = LoadState.Loading,
    val gridColumnCount: GridColumnCount = GridColumnCount.of(3),
    val isSettingsDialogVisible: Boolean = false
) {
    sealed class LoadState {
        data object Loading : LoadState()
        data object PermissionRequired : LoadState()
        data class Success(val mediaList: List<Media>) : LoadState()
        data class Error(val error: Throwable? = null) : LoadState()
    }
}