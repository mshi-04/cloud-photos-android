package com.appvoyager.cloudphotos.ui.auth.testutil

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

internal class LifecycleAuthRepository : AuthRepository {
    override suspend fun signIn(request: SignInRequest): AuthResult<SignInState> =
        AuthResult.Success(SignInState.SignedIn)

    override suspend fun resetPassword(request: ResetPasswordRequest): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun confirmResetPassword(request: ConfirmResetPasswordRequest): AuthResult<Unit> =
        AuthResult.Success(Unit)

    override suspend fun confirmSignUp(request: ConfirmSignUpRequest): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signUp(request: SignUpRequest): AuthResult<Unit> = error("Unexpected signUp")

    override suspend fun resendSignUpCode(request: ResendSignUpCodeRequest): AuthResult<Unit> =
        error("Unexpected resendSignUpCode")

    override suspend fun signOut(): AuthResult<Unit> = error("Unexpected signOut")

    override suspend fun fetchCurrentUser(): AuthResult<AuthUser> = error("Unexpected fetchCurrentUser")

    override suspend fun getSession(): AuthResult<AuthSession> = error("Unexpected getSession")

    override suspend fun deleteUser(): AuthResult<Unit> = error("Unexpected deleteUser")
}
