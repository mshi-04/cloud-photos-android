package com.appvoyager.cloudphotos.domain.auth.model

sealed class AuthResult<out T> {

    data class Success<T>(val value: T) : AuthResult<T>()
    data class Error(val error: AuthError) : AuthResult<Nothing>()

}

fun <T> AuthResult<T>.getOrNull(): T? =
    (this as? AuthResult.Success)?.value

fun <T> AuthResult<T>.errorOrNull(): AuthError? =
    (this as? AuthResult.Error)?.error

inline fun <T, R> AuthResult<T>.map(transform: (T) -> R): AuthResult<R> =
    when (this) {
        is AuthResult.Success -> AuthResult.Success(transform(value))
        is AuthResult.Error -> this
    }

inline fun <T, R> AuthResult<T>.flatMap(
    transform: (T) -> AuthResult<R>
): AuthResult<R> =
    when (this) {
        is AuthResult.Success -> transform(value)
        is AuthResult.Error -> this
    }