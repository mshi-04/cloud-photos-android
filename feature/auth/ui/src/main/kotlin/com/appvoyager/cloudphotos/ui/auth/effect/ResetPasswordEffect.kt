package com.appvoyager.cloudphotos.ui.auth.effect

sealed class ResetPasswordEffect {
    data class NavigateBackToLogin(val message: AuthSnackbarMessage? = null) : ResetPasswordEffect()
    data class ShowSnackbar(val message: AuthSnackbarMessage) : ResetPasswordEffect()
}
