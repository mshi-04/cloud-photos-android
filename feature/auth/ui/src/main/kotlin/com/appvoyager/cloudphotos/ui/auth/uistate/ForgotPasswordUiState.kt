package com.appvoyager.cloudphotos.ui.auth.uistate

import com.appvoyager.cloudphotos.ui.util.ValidationUtils

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val emailError: AuthFieldError? = null
) {
    val isFormValid: Boolean
        get() = email.isNotBlank() && ValidationUtils.isValidEmailFormat(email)
}
