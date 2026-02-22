package com.appvoyager.cloudphotos.data.auth.repository

import com.amplifyframework.core.Amplify
import com.appvoyager.cloudphotos.data.auth.util.AuthErrorMapper
import com.appvoyager.cloudphotos.domain.auth.datasource.AuthDataSource
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.model.AuthUser
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmSignUpRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignInRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignUpRequest
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.UserId
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

class AuthRepositoryImpl @Inject constructor(
    private val dataSource: AuthDataSource,
) : AuthRepository {

    override suspend fun signUp(request: SignUpRequest): AuthResult<Unit> =
        dataSource.signUp(request)

    override suspend fun confirmSignUp(request: ConfirmSignUpRequest): AuthResult<Unit> =
        dataSource.confirmSignUp(request)

    override suspend fun signIn(request: SignInRequest): AuthResult<Unit> =
        dataSource.signIn(request)

    override suspend fun signOut(): AuthResult<Unit> =
        dataSource.signOut()

    override suspend fun fetchCurrentUser(): AuthResult<AuthUser> =
        runCatching {
            val user = suspendCancellableCoroutine { continuation ->
                Amplify.Auth.getCurrentUser(
                    { continuation.resume(it) { _, _, _ -> } },
                    { continuation.resumeWithException(it) }
                )
            }
            AuthUser(
                userId = UserId(user.userId),
                email = Email.of(user.username)
            )
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = { AuthResult.Error(AuthErrorMapper.map(it)) }
        )

    override suspend fun getSession(): AuthResult<AuthSession> =
        dataSource.getSession()

}
