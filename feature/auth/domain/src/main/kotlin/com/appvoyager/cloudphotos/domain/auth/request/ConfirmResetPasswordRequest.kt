package com.appvoyager.cloudphotos.domain.auth.request

import com.appvoyager.cloudphotos.domain.auth.valueobject.ConfirmationCode
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.Password

data class ConfirmResetPasswordRequest(
    val email: Email,
    val code: ConfirmationCode,
    val newPassword: Password
)
