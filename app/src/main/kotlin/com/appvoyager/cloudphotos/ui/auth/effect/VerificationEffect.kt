package com.appvoyager.cloudphotos.ui.auth.effect

sealed class VerificationEffect {
    data object NavigateToHome : VerificationEffect()
    data class ShowSnackbar(val message: String) : VerificationEffect()
}