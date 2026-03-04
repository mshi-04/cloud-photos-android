package com.appvoyager.cloudphotos.ui.auth.uistate

import androidx.annotation.StringRes
import com.appvoyager.cloudphotos.ui.util.ValidationUtils

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    @param:StringRes val emailError: Int? = null
) {
    val isFormValid: Boolean
        get() = email.isNotBlank() && ValidationUtils.isValidEmailFormat(email)
}
