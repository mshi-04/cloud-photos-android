package com.appvoyager.cloudphotos.ui.auth.uistate

data class VerificationCodeUiState(
    val codes: List<String> = List(6) { "" },
    val isLoading: Boolean = false,
    val codeError: AuthFieldError? = null,
    val resendTimerSeconds: Int = DEFAULT_RESEND_COOLDOWN_SECONDS
) {
    companion object {
        const val DEFAULT_RESEND_COOLDOWN_SECONDS = 60
    }
}
