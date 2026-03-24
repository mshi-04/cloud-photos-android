package com.appvoyager.cloudphotos.ui

sealed interface MainUiState {
    data object Authenticated : MainUiState
    data object Unauthenticated : MainUiState
}