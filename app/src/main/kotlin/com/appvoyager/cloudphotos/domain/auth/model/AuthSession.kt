package com.appvoyager.cloudphotos.domain.auth.model

import com.appvoyager.cloudphotos.domain.auth.valueobject.JwtToken

data class AuthSession(
    val isSignedIn: Boolean,
    val accessToken: JwtToken?,
    val idToken: JwtToken?,
    val refreshToken: JwtToken?,
) {
    val isGuest: Boolean get() = !isSignedIn
}