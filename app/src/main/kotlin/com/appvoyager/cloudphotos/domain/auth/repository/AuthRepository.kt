package com.appvoyager.cloudphotos.domain.auth.repository

import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.model.AuthUser
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmSignUpRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignInRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignUpRequest

interface AuthRepository {

    suspend fun signUp(request: SignUpRequest): AuthResult<Unit>

    suspend fun confirmSignUp(request: ConfirmSignUpRequest): AuthResult<Unit>

    suspend fun signIn(request: SignInRequest): AuthResult<SignInState>

    suspend fun signOut(): AuthResult<Unit>

    suspend fun fetchCurrentUser(): AuthResult<AuthUser>

    suspend fun getSession(): AuthResult<AuthSession>

}
