package com.appvoyager.cloudphotos.ui.auth.effect

import androidx.annotation.StringRes

sealed class ResetPasswordCodeEffect {
    data class NavigateBackToLogin(
        @param:StringRes val messageResId: Int? = null
    ) : ResetPasswordCodeEffect()

    data class ShowSnackbar(@param:StringRes val messageResId: Int) : ResetPasswordCodeEffect()
}
