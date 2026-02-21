package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.request.SignUpRequest
import javax.inject.Inject

class SignUpUseCase @Inject constructor (
    private val repository: AuthRepository
) {

    suspend operator fun invoke(request: SignUpRequest): AuthResult<Unit> =
        repository.signUp(request)

}
