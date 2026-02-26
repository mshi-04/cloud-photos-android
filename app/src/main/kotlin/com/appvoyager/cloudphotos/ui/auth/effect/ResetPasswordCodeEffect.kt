package com.appvoyager.cloudphotos.ui.auth.effect

sealed class ResetPasswordCodeEffect {
    data object NavigateBackToLogin : ResetPasswordCodeEffect()
    data class ShowSnackbar(val message: String) : ResetPasswordCodeEffect()
}
