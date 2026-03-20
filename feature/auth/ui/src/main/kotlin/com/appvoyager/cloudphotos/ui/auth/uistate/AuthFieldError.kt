package com.appvoyager.cloudphotos.ui.auth.uistate

sealed class AuthFieldError {
    data object InvalidEmail : AuthFieldError()
    data object PasswordTooShort : AuthFieldError()
    data object InvalidCredentials : AuthFieldError()
    data object InvalidPassword : AuthFieldError()
    data object EmailAlreadyRegistered : AuthFieldError()
    data object EnterCode : AuthFieldError()
    data object CodeMismatch : AuthFieldError()
    data object CodeExpired : AuthFieldError()
    data object CheckInput : AuthFieldError()
    data object ConfirmCode : AuthFieldError()
}
