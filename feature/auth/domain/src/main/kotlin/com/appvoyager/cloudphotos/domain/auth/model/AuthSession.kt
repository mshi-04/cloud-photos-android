package com.appvoyager.cloudphotos.domain.auth.model

enum class AuthState { SignedIn, Guest }

data class AuthSession(
    val state: AuthState,
) {
    val isSignedIn: Boolean get() = state == AuthState.SignedIn
    val isGuest: Boolean get() = state == AuthState.Guest
}