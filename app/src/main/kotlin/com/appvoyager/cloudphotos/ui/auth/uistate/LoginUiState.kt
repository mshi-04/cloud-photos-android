package com.appvoyager.cloudphotos.ui.auth.uistate

import androidx.annotation.StringRes

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    @get:StringRes val emailError: Int? = null,
    @get:StringRes val passwordError: Int? = null
)
