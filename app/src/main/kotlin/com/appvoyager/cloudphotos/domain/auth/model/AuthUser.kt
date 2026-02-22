package com.appvoyager.cloudphotos.domain.auth.model

import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.UserId

data class AuthUser(
    val userId: UserId,
    val email: Email?
)