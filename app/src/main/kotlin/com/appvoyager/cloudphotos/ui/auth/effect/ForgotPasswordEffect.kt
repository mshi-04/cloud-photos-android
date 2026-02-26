package com.appvoyager.cloudphotos.ui.auth.effect

sealed class ForgotPasswordEffect {
    data class NavigateToResetCode(val email: String) : ForgotPasswordEffect()
    data object NavigateBackToLogin : ForgotPasswordEffect()
    data class ShowSnackbar(val message: String) : ForgotPasswordEffect()
}
