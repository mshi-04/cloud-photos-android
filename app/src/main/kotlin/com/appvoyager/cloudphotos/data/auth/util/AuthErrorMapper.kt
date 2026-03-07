package com.appvoyager.cloudphotos.data.auth.util

import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeMismatchException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ExpiredCodeException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InvalidPasswordException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.LimitExceededException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.TooManyRequestsException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserNotConfirmedException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UsernameExistsException
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.exceptions.ValidationException
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import java.io.IOException

internal object AuthErrorMapper {

    fun map(throwable: Throwable): AuthError =
        when (throwable) {
            is NotAuthorizedException -> AuthError.InvalidCredentials(throwable.message)
            is SessionExpiredException -> AuthError.InvalidCredentials(throwable.message)
            is SignedOutException -> AuthError.InvalidCredentials(throwable.message)
            is IOException -> AuthError.Network(throwable.message)
            is ServiceException -> mapServiceException(throwable)
            is ValidationException -> mapValidationException(throwable)
            else -> AuthError.Unknown(throwable.message)
        }

    private fun mapServiceException(exception: ServiceException): AuthError =
        when (val cause = exception.cause) {
            is ExpiredCodeException -> AuthError.CodeExpired(cause.message)
            is CodeMismatchException -> AuthError.CodeMismatch(cause.message)
            is NotAuthorizedException -> AuthError.InvalidCredentials(cause.message)
            is InvalidPasswordException -> AuthError.InvalidPassword(cause.message)
            is LimitExceededException -> AuthError.TooManyRequests(cause.message)
            is TooManyRequestsException -> AuthError.TooManyRequests(cause.message)
            is UserNotConfirmedException -> AuthError.UserNotConfirmed(cause.message)
            is UsernameExistsException -> AuthError.UsernameAlreadyExists(cause.message)
            else -> AuthError.Unknown(exception.message)
        }

    private fun mapValidationException(exception: ValidationException): AuthError {
        val message = exception.message ?: ""
        return when {
            message.contains("code", ignoreCase = true) -> AuthError.CodeMismatch(exception.message)
            else -> AuthError.Unknown(exception.message)
        }
    }

}
