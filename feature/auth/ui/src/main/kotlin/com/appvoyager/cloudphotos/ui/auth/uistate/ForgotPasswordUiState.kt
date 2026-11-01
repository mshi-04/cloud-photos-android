package com.appvoyager.cloudphotos.ui.auth.uistate

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val emailError: AuthFieldError? = null
)
