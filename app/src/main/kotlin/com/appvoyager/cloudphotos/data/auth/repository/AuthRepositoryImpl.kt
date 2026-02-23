package com.appvoyager.cloudphotos.data.auth.repository

import com.appvoyager.cloudphotos.domain.auth.datasource.AuthDataSource
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.model.AuthUser
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmSignUpRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignInRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignUpRequest
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val dataSource: AuthDataSource,
) : AuthRepository {

    override suspend fun signUp(request: SignUpRequest): AuthResult<Unit> =
        dataSource.signUp(request)

    override suspend fun confirmSignUp(request: ConfirmSignUpRequest): AuthResult<Unit> =
        dataSource.confirmSignUp(request)

    override suspend fun signIn(request: SignInRequest): AuthResult<SignInState> =
        dataSource.signIn(request)

    override suspend fun signOut(): AuthResult<Unit> =
        dataSource.signOut()

    override suspend fun fetchCurrentUser(): AuthResult<AuthUser> =
        dataSource.fetchCurrentUser()

    override suspend fun getSession(): AuthResult<AuthSession> =
        dataSource.getSession()

}
