package com.appvoyager.cloudphotos.ui.auth.effect

import androidx.annotation.StringRes

sealed class ResetPasswordEffect {
    data class NavigateBackToLogin(
        @param:StringRes val messageResId: Int? = null
    ) : ResetPasswordEffect()

    data class ShowSnackbar(@param:StringRes val messageResId: Int) : ResetPasswordEffect()
}
