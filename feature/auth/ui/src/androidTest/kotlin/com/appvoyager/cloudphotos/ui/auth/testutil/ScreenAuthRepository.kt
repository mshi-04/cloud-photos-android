package com.appvoyager.cloudphotos.ui.auth.testutil

import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.model.AuthUser
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmSignUpRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResendSignUpCodeRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignInRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignUpRequest

internal class ScreenAuthRepository : AuthRepository {

    override suspend fun signUp(request: SignUpRequest): AuthResult<Unit> = error("Unexpected signUp")

    override suspend fun confirmSignUp(request: ConfirmSignUpRequest): AuthResult<Unit> =
        AuthResult.Error(AuthError.CodeMismatch())

    override suspend fun signIn(request: SignInRequest): AuthResult<SignInState> = error("Unexpected signIn")

    override suspend fun signOut(): AuthResult<Unit> = error("Unexpected signOut")

    override suspend fun fetchCurrentUser(): AuthResult<AuthUser> = error("Unexpected fetchCurrentUser")

    override suspend fun getSession(): AuthResult<AuthSession> = error("Unexpected getSession")

    override suspend fun resendSignUpCode(request: ResendSignUpCodeRequest): AuthResult<Unit> =
        error("Unexpected resendSignUpCode")

    override suspend fun resetPassword(request: ResetPasswordRequest): AuthResult<Unit> =
        error("Unexpected resetPassword")

    override suspend fun confirmResetPassword(request: ConfirmResetPasswordRequest): AuthResult<Unit> =
        error("Unexpected confirmResetPassword")

    override suspend fun deleteUser(): AuthResult<Unit> = error("Unexpected deleteUser")
}
