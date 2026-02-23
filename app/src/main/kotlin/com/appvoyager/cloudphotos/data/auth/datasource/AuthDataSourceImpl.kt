package com.appvoyager.cloudphotos.data.auth.datasource

import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.core.Amplify
import com.appvoyager.cloudphotos.data.auth.util.AuthErrorMapper
import com.appvoyager.cloudphotos.data.auth.util.AuthSignInStepMapper
import com.appvoyager.cloudphotos.domain.auth.datasource.AuthDataSource
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.model.AuthState
import com.appvoyager.cloudphotos.domain.auth.model.AuthUser
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmSignUpRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignInRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignUpRequest
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.UserId
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

class AuthDataSourceImpl @Inject constructor() : AuthDataSource {

    override suspend fun signUp(request: SignUpRequest): AuthResult<Unit> =
        runCatching {
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

    override suspend fun confirmSignUp(request: ConfirmSignUpRequest): AuthResult<Unit> =
        runCatching {
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

    override suspend fun signIn(request: SignInRequest): AuthResult<SignInState> =
        runCatching {
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

    override suspend fun signOut(): AuthResult<Unit> =
        runCatching {
            val result = suspendCancellableCoroutine { coroutine ->
                Amplify.Auth.signOut { signOutResult ->
                    coroutine.resume(signOutResult) { _, _, _ -> }
                }
            }

            when (result) {
                is AWSCognitoAuthSignOutResult.CompleteSignOut -> Unit
                is AWSCognitoAuthSignOutResult.PartialSignOut -> {
                    throw result.globalSignOutError?.exception
                        ?: result.hostedUIError?.exception
                        ?: result.revokeTokenError?.exception
                        ?: Exception("Partial sign-out: local success but remote operations failed")
                }

                is AWSCognitoAuthSignOutResult.FailedSignOut -> {
                    throw result.exception
                }

                else -> throw IllegalStateException("Unknown sign-out result type: ${result::class}")
            }
        }.fold(
            onSuccess = { AuthResult.Success(Unit) },
            onFailure = { AuthResult.Error(AuthErrorMapper.map(it)) }
        )

    override suspend fun fetchCurrentUser(): AuthResult<AuthUser> =
        runCatching {
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
                userId = UserId(user.userId),
                email = Email.of(emailAttribute.value)
            )
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = { AuthResult.Error(AuthErrorMapper.map(it)) }
        )

    override suspend fun getSession(): AuthResult<AuthSession> =
        runCatching {
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

}
