package com.appvoyager.cloudphotos.domain.auth.model

sealed class AuthError(open val message: String?) {

    data class CodeExpired(override val message: String? = null) : AuthError(message)

    data class CodeMismatch(override val message: String? = null) : AuthError(message)

    data class InvalidCredentials(override val message: String? = null) : AuthError(message)

    data class InvalidPassword(override val message: String? = null) : AuthError(message)

    data class Network(override val message: String? = null) : AuthError(message)

    data class TooManyRequests(override val message: String? = null) : AuthError(message)

    data class Unknown(override val message: String? = null) : AuthError(message)

    data class UserNotConfirmed(override val message: String? = null) : AuthError(message)

    data class UsernameAlreadyExists(override val message: String? = null) : AuthError(message)
}
