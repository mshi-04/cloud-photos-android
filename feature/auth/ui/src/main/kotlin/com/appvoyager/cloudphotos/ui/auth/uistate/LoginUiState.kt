package com.appvoyager.cloudphotos.ui.auth.uistate

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: AuthFieldError? = null,
    val passwordError: AuthFieldError? = null
)
