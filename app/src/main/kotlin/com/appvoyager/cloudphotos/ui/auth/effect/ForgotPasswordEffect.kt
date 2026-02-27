package com.appvoyager.cloudphotos.ui.auth.effect

import com.appvoyager.cloudphotos.domain.auth.valueobject.Email

sealed class ForgotPasswordEffect {
    data class NavigateToResetCode(val email: Email) : ForgotPasswordEffect()
    data object NavigateBackToLogin : ForgotPasswordEffect()
    data class ShowSnackbar(val message: String) : ForgotPasswordEffect()
}
