package com.appvoyager.cloudphotos.ui

sealed interface MainUiState {
    data object None : MainUiState
    data object Authenticated : MainUiState
    data object Unauthenticated : MainUiState
    data object SessionCheckError : MainUiState
}
