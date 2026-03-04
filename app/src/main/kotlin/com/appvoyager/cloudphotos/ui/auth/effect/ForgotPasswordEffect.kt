package com.appvoyager.cloudphotos.ui.auth.effect

import androidx.annotation.StringRes
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email

sealed class ForgotPasswordEffect {
    data class NavigateToResetCode(val email: Email) : ForgotPasswordEffect()
    data object NavigateBackToLogin : ForgotPasswordEffect()
    data class ShowSnackbar(@param:StringRes val messageResId: Int) : ForgotPasswordEffect()
}
