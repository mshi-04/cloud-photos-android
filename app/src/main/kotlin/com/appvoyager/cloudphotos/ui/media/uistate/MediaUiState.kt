package com.appvoyager.cloudphotos.ui.media.uistate

import com.appvoyager.cloudphotos.domain.media.model.Media
import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount

data class MediaUiState(
    val mediaList: List<Media> = emptyList(),
    val gridColumnCount: GridColumnCount = GridColumnCount.of(3),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val isSettingsDialogVisible: Boolean = false
)
