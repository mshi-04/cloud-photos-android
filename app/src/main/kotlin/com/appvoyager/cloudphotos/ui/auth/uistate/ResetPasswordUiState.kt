package com.appvoyager.cloudphotos.ui.auth.uistate

import androidx.annotation.StringRes

data class ResetPasswordUiState(
    val codes: List<String> = List(6) { "" },
    val newPassword: String = "",
    val isNewPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    @param:StringRes val codeError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    val resendTimerSeconds: Int = DEFAULT_RESEND_COOLDOWN_SECONDS
) {
    val isCodeComplete: Boolean
        get() = codes.size == 6 && codes.all { it.length == 1 && it[0].isDigit() }

    val isFormValid: Boolean
        get() = isCodeComplete && newPassword.length >= 8

    val isResendEnabled: Boolean
        get() = resendTimerSeconds <= 0 && !isLoading

    companion object {
        const val DEFAULT_RESEND_COOLDOWN_SECONDS = 60
    }
}
