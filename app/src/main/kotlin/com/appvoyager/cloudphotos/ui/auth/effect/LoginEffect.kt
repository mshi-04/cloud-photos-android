package com.appvoyager.cloudphotos.ui.auth.effect

sealed class LoginEffect {
    data class NavigateToVerification(val email: String) : LoginEffect()
    data object NavigateToHome : LoginEffect()
    data object NavigateToForgotPassword : LoginEffect()
    data class ShowSnackbar(val message: String) : LoginEffect()
}