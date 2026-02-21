package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.AuthUser
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.request.SignInRequest

class SignInUseCase(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(request: SignInRequest): AuthResult<AuthUser> =
        repository.signIn(request)

}
