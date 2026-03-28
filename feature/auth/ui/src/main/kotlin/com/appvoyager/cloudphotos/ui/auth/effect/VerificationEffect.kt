package com.appvoyager.cloudphotos.ui.auth.effect

sealed class VerificationEffect {
    data object NavigateToHome : VerificationEffect()
    data object NavigateBack : VerificationEffect()
    data class ShowSnackbar(val message: AuthSnackbarMessage) : VerificationEffect()
}
