package com.appvoyager.cloudphotos.data.auth.util

import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeMismatchException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ExpiredCodeException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.TooManyRequestsException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserNotConfirmedException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UsernameExistsException
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import java.io.IOException

internal object AuthErrorMapper {

    fun map(throwable: Throwable): AuthError =
        when (throwable) {
            is NotAuthorizedException -> AuthError.InvalidCredentials(throwable.message)
            is UserNotConfirmedException -> AuthError.UserNotConfirmed(throwable.message)
            is UsernameExistsException -> AuthError.UsernameAlreadyExists(throwable.message)
            is CodeMismatchException -> AuthError.CodeMismatch(throwable.message)
            is ExpiredCodeException -> AuthError.CodeExpired(throwable.message)
            is TooManyRequestsException -> AuthError.TooManyRequests(throwable.message)
            is IOException -> AuthError.Network(throwable.message)
            else -> AuthError.Unknown(throwable.message)
        }

}
