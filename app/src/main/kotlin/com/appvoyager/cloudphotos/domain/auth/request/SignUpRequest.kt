package com.appvoyager.cloudphotos.domain.auth.request

import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.Password

data class SignUpRequest(
    val email: Email,
    val password: Password
)