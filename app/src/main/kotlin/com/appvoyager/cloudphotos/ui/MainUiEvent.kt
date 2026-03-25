package com.appvoyager.cloudphotos.ui

sealed interface MainUiEvent {
    data object SignOutFailed : MainUiEvent
}
