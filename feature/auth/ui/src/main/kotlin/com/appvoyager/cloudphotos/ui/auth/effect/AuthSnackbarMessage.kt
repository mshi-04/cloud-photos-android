package com.appvoyager.cloudphotos.ui.auth.effect

sealed class AuthSnackbarMessage {
    data object Network : AuthSnackbarMessage()
    data object TooManyRequests : AuthSnackbarMessage()
    data object Unknown : AuthSnackbarMessage()
    data object AdditionalAuthRequired : AuthSnackbarMessage()
    data object InvalidPassword : AuthSnackbarMessage()
    data object InvalidCredentials : AuthSnackbarMessage()
    data object CodeExpired : AuthSnackbarMessage()
    data object CodeMismatch : AuthSnackbarMessage()
    data object PasswordReset : AuthSnackbarMessage()
    data object CodeResent : AuthSnackbarMessage()
    data object ResendFailed : AuthSnackbarMessage()

    val key: String get() = javaClass.simpleName

    companion object {
        fun fromKey(key: String): AuthSnackbarMessage? = when (key) {
            "Network" -> Network
            "TooManyRequests" -> TooManyRequests
            "Unknown" -> Unknown
            "AdditionalAuthRequired" -> AdditionalAuthRequired
            "InvalidPassword" -> InvalidPassword
            "InvalidCredentials" -> InvalidCredentials
            "CodeExpired" -> CodeExpired
            "CodeMismatch" -> CodeMismatch
            "PasswordReset" -> PasswordReset
            "CodeResent" -> CodeResent
            "ResendFailed" -> ResendFailed
            else -> null
        }
    }
}
