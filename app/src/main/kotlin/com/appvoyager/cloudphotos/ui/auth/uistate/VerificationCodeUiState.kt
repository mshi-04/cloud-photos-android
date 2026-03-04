package com.appvoyager.cloudphotos.ui.auth.uistate

import androidx.annotation.StringRes
import com.appvoyager.cloudphotos.ui.auth.viewmodel.VerificationCodeViewModel

data class VerificationCodeUiState(
    val codes: List<String> = List(6) { "" },
    val isLoading: Boolean = false,
    @param:StringRes val codeError: Int? = null,
    val resendTimerSeconds: Int = VerificationCodeViewModel.RESEND_COOLDOWN_SECONDS
)
