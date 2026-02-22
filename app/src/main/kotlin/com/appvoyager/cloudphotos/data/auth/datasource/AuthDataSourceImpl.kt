package com.appvoyager.cloudphotos.data.auth.datasource

import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.core.Amplify
import com.appvoyager.cloudphotos.data.auth.util.AuthErrorMapper
import com.appvoyager.cloudphotos.domain.auth.datasource.AuthDataSource
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.model.AuthState
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmSignUpRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignInRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignUpRequest
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

    override suspend fun signIn(request: SignInRequest): AuthResult<Unit> =
        runCatching {
            val result = suspendCancellableCoroutine { coroutine ->
                Amplify.Auth.signIn(
                    request.email.value,
                    request.password.value,
                    { coroutine.resume(it) { _, _, _ -> } },
                    { coroutine.resumeWithException(it) }
                )
            }
            if (!result.isSignedIn) {
                throw IllegalStateException(
                    "Sign-in not complete: ${result.nextStep.signInStep}"
                )
            }
            Unit
        }.fold(
            onSuccess = { AuthResult.Success(Unit) },
            onFailure = { AuthResult.Error(AuthErrorMapper.map(it)) }
        )

    override suspend fun signOut(): AuthResult<Unit> =
        runCatching {
            suspendCancellableCoroutine { coroutine ->
                Amplify.Auth.signOut({ coroutine.resume(Unit) { _, _, _ -> } })
            }
        }.fold(
            onSuccess = { AuthResult.Success(Unit) },
            onFailure = { AuthResult.Error(AuthErrorMapper.map(it)) },
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
