package com.appvoyager.cloudphotos.ui.auth.effect

import androidx.annotation.StringRes

sealed class VerificationEffect {
    data object NavigateToHome : VerificationEffect()
    data class ShowSnackbar(@param:StringRes val messageResId: Int) : VerificationEffect()
}