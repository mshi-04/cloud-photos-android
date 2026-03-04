package com.appvoyager.cloudphotos.ui.auth.uistate

import androidx.annotation.StringRes

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    @param:StringRes val emailError: Int? = null
)
