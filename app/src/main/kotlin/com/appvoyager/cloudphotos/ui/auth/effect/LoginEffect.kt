package com.appvoyager.cloudphotos.ui.auth.effect

import com.appvoyager.cloudphotos.domain.auth.valueobject.Email

sealed class LoginEffect {
    data class NavigateToVerification(val email: Email) : LoginEffect()
    data object NavigateToHome : LoginEffect()
    data object NavigateToForgotPassword : LoginEffect()
    data class ShowSnackbar(val message: String) : LoginEffect()
}