package com.appvoyager.cloudphotos.data.auth.datasource

import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.core.Amplify
import com.appvoyager.cloudphotos.data.auth.util.AuthErrorMapper
import com.appvoyager.cloudphotos.data.auth.util.AuthSignInStepMapper
import com.appvoyager.cloudphotos.data.common.awaitAmplifyRestCall
import com.appvoyager.cloudphotos.data.fcm.DeviceToken
import com.appvoyager.cloudphotos.data.fcm.DeviceTokenDataSource
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.model.AuthState
import com.appvoyager.cloudphotos.domain.auth.model.AuthUser
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmSignUpRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResendSignUpCodeRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignInRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignUpRequest
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.UserId
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class AuthDataSourceImpl @Inject constructor(private val deviceTokenDataSource: DeviceTokenDataSource) :
    AuthDataSource {

    override suspend fun signUp(request: SignUpRequest): AuthResult<Unit> = runCatching {
        val options = AuthSignUpOptions.builder()
            .userAttribute(AuthUserAttributeKey.email(), request.email.value)
            .build()

        suspendCancellableCoroutine { coroutine ->
            Amplify.Auth.signUp(
                request.email.value,
                request.password.value,
                options,
                { coroutine.resume(Unit) { _, _, _ -> } },
                { coroutine.resumeWithException(it) }
            )
        }
    }.fold(
        onSuccess = { AuthResult.Success(Unit) },
        onFailure = { AuthResult.Error(AuthErrorMapper.map(it)) }
    )

    override suspend fun confirmSignUp(request: ConfirmSignUpRequest): AuthResult<Unit> = runCatching {
        suspendCancellableCoroutine { coroutine ->
            Amplify.Auth.confirmSignUp(
                request.email.value,
                request.code.value,
                { coroutine.resume(Unit) { _, _, _ -> } },
                { coroutine.resumeWithException(it) }
            )
        }
    }.fold(
        onSuccess = { AuthResult.Success(Unit) },
        onFailure = { AuthResult.Error(AuthErrorMapper.map(it)) }
    )

    override suspend fun signIn(request: SignInRequest): AuthResult<SignInState> = runCatching {
        val result = suspendCancellableCoroutine { coroutine ->
            Amplify.Auth.signIn(
                request.email.value,
                request.password.value,
                { coroutine.resume(it) { _, _, _ -> } },
                { coroutine.resumeWithException(it) }
            )
        }
        if (result.isSignedIn) {
            SignInState.SignedIn
        } else {
            val nextStep = result.nextStep
            when (nextStep.signInStep) {
                AuthSignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE,
                AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE,
                AuthSignInStep.CONFIRM_SIGN_IN_WITH_TOTP_CODE,
                AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION,
                AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP -> {
                    SignInState.MFARequired(
                        AuthSignInStepMapper.mapSignInStep(nextStep.signInStep)
                    )
                }

                AuthSignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD -> {
                    SignInState.NewPasswordRequired(
                        AuthSignInStepMapper.mapSignInStep(nextStep.signInStep)
                    )
                }

                AuthSignInStep.DONE -> SignInState.SignedIn

                else -> {
                    SignInState.AdditionalStepRequired(
                        AuthSignInStepMapper.mapSignInStep(nextStep.signInStep)
                    )
                }
            }
        }
    }.fold(
        onSuccess = { AuthResult.Success(it) },
        onFailure = { AuthResult.Error(AuthErrorMapper.map(it)) }
    )

    override suspend fun signOut(): AuthResult<Unit> {
        runCatching { cleanUpFcmToken() }
            .onFailure { if (it is CancellationException) throw it }

        val result = suspendCancellableCoroutine { coroutine ->
            Amplify.Auth.signOut { coroutine.resume(it) { _, _, _ -> } }
        }

        return when (result) {
            is AWSCognitoAuthSignOutResult.CompleteSignOut -> AuthResult.Success(Unit)
            is AWSCognitoAuthSignOutResult.PartialSignOut -> {
                AuthResult.Success(Unit)
            }

            is AWSCognitoAuthSignOutResult.FailedSignOut ->
                AuthResult.Error(AuthErrorMapper.map(result.exception))

            else -> AuthResult.Error(
                AuthErrorMapper.map(IllegalStateException("Unknown sign-out result: ${result::class}"))
            )
        }
    }

    override suspend fun fetchCurrentUser(): AuthResult<AuthUser> = runCatching {
        val user = suspendCancellableCoroutine { continuation ->
            Amplify.Auth.getCurrentUser(
                { continuation.resume(it) { _, _, _ -> } },
                { continuation.resumeWithException(it) }
            )
        }

        val attributes = suspendCancellableCoroutine { continuation ->
            Amplify.Auth.fetchUserAttributes(
                { continuation.resume(it) { _, _, _ -> } },
                { continuation.resumeWithException(it) }
            )
        }

        val emailAttribute = attributes.find { it.key == AuthUserAttributeKey.email() }
            ?: throw IllegalStateException("Email attribute not found for current user")

        AuthUser(
            userId = UserId.of(user.userId),
            email = Email.of(emailAttribute.value)
        )
    }.fold(
        onSuccess = { AuthResult.Success(it) },
        onFailure = { AuthResult.Error(AuthErrorMapper.map(it)) }
    )

    override suspend fun getSession(): AuthResult<AuthSession> = runCatching {
        val signedIn = suspendCancellableCoroutine { coroutine ->
            Amplify.Auth.fetchAuthSession(
                { session ->
                    coroutine.resume(session.isSignedIn) { _, _, _ -> }
                },
                { error -> coroutine.resumeWithException(error) }
            )
        }
        AuthSession(
            state = if (signedIn) AuthState.SignedIn else AuthState.Guest
        )
    }.fold(
        onSuccess = { AuthResult.Success(it) },
        onFailure = { AuthResult.Error(AuthErrorMapper.map(it)) }
    )

    override suspend fun resendSignUpCode(request: ResendSignUpCodeRequest): AuthResult<Unit> = runCatching {
        suspendCancellableCoroutine { coroutine ->
            Amplify.Auth.resendSignUpCode(
                request.email.value,
                { coroutine.resume(Unit) { _, _, _ -> } },
                { coroutine.resumeWithException(it) }
            )
        }
    }.fold(
        onSuccess = { AuthResult.Success(Unit) },
        onFailure = { AuthResult.Error(AuthErrorMapper.map(it)) }
    )

    override suspend fun resetPassword(request: ResetPasswordRequest): AuthResult<Unit> = runCatching {
        suspendCancellableCoroutine { coroutine ->
            Amplify.Auth.resetPassword(
                request.email.value,
                { coroutine.resume(Unit) { _, _, _ -> } },
                { coroutine.resumeWithException(it) }
            )
        }
    }.fold(
        onSuccess = { AuthResult.Success(Unit) },
        onFailure = { AuthResult.Error(AuthErrorMapper.map(it)) }
    )

    override suspend fun confirmResetPassword(request: ConfirmResetPasswordRequest): AuthResult<Unit> = runCatching {
        suspendCancellableCoroutine { coroutine ->
            Amplify.Auth.confirmResetPassword(
                request.email.value,
                request.newPassword.value,
                request.code.value,
                { coroutine.resume(Unit) { _, _, _ -> } },
                { error -> coroutine.resumeWithException(error) }
            )
        }
    }.fold(
        onSuccess = { AuthResult.Success(Unit) },
        onFailure = { AuthResult.Error(AuthErrorMapper.map(it)) }
    )

    override suspend fun deleteUser(): AuthResult<Unit> {
        runCatching { cleanUpFcmToken() }
            .onFailure { it.rethrowIfCancellation() }

        val options = RestOptions.builder()
            .addPath("/users")
            .build()
        // Backend deletion must succeed before Cognito account is removed.
        // If the order were reversed, the auth token would be invalid and the backend call would fail,
        // leaving S3/DynamoDB data orphaned.
        val restResult = runCatching {
            awaitAmplifyRestCall(options) { name, opts, onResp, onErr ->
                Amplify.API.delete(name, opts, onResp, onErr)
            }
        }
        val restFailure = restResult.exceptionOrNull()
        if (restFailure != null) {
            restFailure.rethrowIfCancellation()
            return AuthResult.Error(AuthErrorMapper.map(restFailure))
        }

        return runCatching {
            suspendCancellableCoroutine { coroutine ->
                Amplify.Auth.deleteUser(
                    { coroutine.resume(Unit) { _, _, _ -> } },
                    { coroutine.resumeWithException(it) }
                )
            }
        }.fold(
            onSuccess = { AuthResult.Success(Unit) },
            onFailure = {
                it.rethrowIfCancellation()
                AuthResult.Error(AuthErrorMapper.map(it))
            }
        )
    }

    private fun Throwable.rethrowIfCancellation() {
        if (this is CancellationException) throw this
    }

    private suspend fun cleanUpFcmToken() {
        try {
            val rawToken = suspendCancellableCoroutine { coroutine ->
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { coroutine.resume(it) { _, _, _ -> } }
                    .addOnFailureListener { coroutine.resumeWithException(it) }
            }
            val token = DeviceToken.of(rawToken)
            deviceTokenDataSource.unregister(token)
        } finally {
            suspendCancellableCoroutine { coroutine ->
                FirebaseMessaging.getInstance().deleteToken()
                    .addOnSuccessListener { coroutine.resume(Unit) { _, _, _ -> } }
                    .addOnFailureListener { coroutine.resumeWithException(it) }
            }
        }
    }
}
