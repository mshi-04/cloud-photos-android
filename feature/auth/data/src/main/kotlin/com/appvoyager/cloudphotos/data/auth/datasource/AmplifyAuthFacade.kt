package com.appvoyager.cloudphotos.data.auth.datasource

import com.amplifyframework.auth.AuthCategoryBehavior
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.core.Amplify
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

internal class AmplifyAuthFacade(private val auth: AuthCategoryBehavior = Amplify.Auth) {

    suspend fun signUp(email: String, password: String, options: AuthSignUpOptions): Unit =
        suspendCancellableCoroutine { continuation ->
            auth.signUp(
                email,
                password,
                options,
                { continuation.resume(Unit) { _, _, _ -> } },
                { continuation.resumeWithException(it) }
            )
        }

    suspend fun confirmSignUp(email: String, code: String): Unit = suspendCancellableCoroutine { continuation ->
        auth.confirmSignUp(
            email,
            code,
            { continuation.resume(Unit) { _, _, _ -> } },
            { continuation.resumeWithException(it) }
        )
    }

    suspend fun signIn(email: String, password: String): AuthSignInResult =
        suspendCancellableCoroutine { continuation ->
            auth.signIn(
                email,
                password,
                { continuation.resume(it) { _, _, _ -> } },
                { continuation.resumeWithException(it) }
            )
        }

    suspend fun getCurrentUser(): AuthUser = suspendCancellableCoroutine { continuation ->
        auth.getCurrentUser(
            { continuation.resume(it) { _, _, _ -> } },
            { continuation.resumeWithException(it) }
        )
    }

    suspend fun fetchUserAttributes(): List<AuthUserAttribute> = suspendCancellableCoroutine { continuation ->
        auth.fetchUserAttributes(
            { continuation.resume(it) { _, _, _ -> } },
            { continuation.resumeWithException(it) }
        )
    }

    suspend fun fetchAuthSession(): AuthSession = suspendCancellableCoroutine { continuation ->
        auth.fetchAuthSession(
            { continuation.resume(it) { _, _, _ -> } },
            { continuation.resumeWithException(it) }
        )
    }

    suspend fun resendSignUpCode(email: String): Unit = suspendCancellableCoroutine { continuation ->
        auth.resendSignUpCode(
            email,
            { continuation.resume(Unit) { _, _, _ -> } },
            { continuation.resumeWithException(it) }
        )
    }

    suspend fun resetPassword(email: String): Unit = suspendCancellableCoroutine { continuation ->
        auth.resetPassword(
            email,
            { continuation.resume(Unit) { _, _, _ -> } },
            { continuation.resumeWithException(it) }
        )
    }

    suspend fun confirmResetPassword(email: String, newPassword: String, code: String): Unit =
        suspendCancellableCoroutine { continuation ->
            auth.confirmResetPassword(
                email,
                newPassword,
                code,
                { continuation.resume(Unit) { _, _, _ -> } },
                { continuation.resumeWithException(it) }
            )
        }
}
