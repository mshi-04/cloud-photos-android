package com.appvoyager.cloudphotos.domain.auth.request

import com.appvoyager.cloudphotos.domain.auth.valueobject.ConfirmationCode
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email

data class ConfirmSignUpRequest(
    val email: Email,
    val code: ConfirmationCode
)