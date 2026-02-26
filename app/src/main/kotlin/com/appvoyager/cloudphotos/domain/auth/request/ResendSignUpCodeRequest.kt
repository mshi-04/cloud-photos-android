package com.appvoyager.cloudphotos.domain.auth.request

import com.appvoyager.cloudphotos.domain.auth.valueobject.Email

data class ResendSignUpCodeRequest(
    val email: Email
)
