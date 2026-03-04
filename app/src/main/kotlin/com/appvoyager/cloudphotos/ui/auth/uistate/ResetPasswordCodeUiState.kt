package com.appvoyager.cloudphotos.ui.auth.uistate

import androidx.annotation.StringRes
import com.appvoyager.cloudphotos.ui.auth.viewmodel.ResetPasswordCodeViewModel

data class ResetPasswordCodeUiState(
    val codes: List<String> = List(6) { "" },
    val newPassword: String = "",
    val isNewPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    @param:StringRes val codeError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    val resendTimerSeconds: Int = ResetPasswordCodeViewModel.RESEND_COOLDOWN_SECONDS
)
