package com.appvoyager.cloudphotos.domain.auth.datasource

import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.model.AuthUser
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmSignUpRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResendSignUpCodeRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignInRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignUpRequest
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email

interface AuthDataSource {

    suspend fun signUp(request: SignUpRequest): AuthResult<Unit>

    suspend fun confirmSignUp(request: ConfirmSignUpRequest): AuthResult<Unit>

    suspend fun signIn(request: SignInRequest): AuthResult<SignInState>

    suspend fun signOut(): AuthResult<Unit>

    suspend fun fetchCurrentUser(): AuthResult<AuthUser>

    suspend fun getSession(): AuthResult<AuthSession>

    suspend fun resendSignUpCode(request: ResendSignUpCodeRequest): AuthResult<Unit>

    suspend fun resetPassword(request: ResetPasswordRequest): AuthResult<Unit>

    suspend fun confirmResetPassword(request: ConfirmResetPasswordRequest): AuthResult<Unit>

}
