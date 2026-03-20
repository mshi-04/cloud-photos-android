package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.request.ResendSignUpCodeRequest
import javax.inject.Inject

class ResendSignUpCodeUseCase @Inject constructor(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(request: ResendSignUpCodeRequest): AuthResult<Unit> =
        repository.resendSignUpCode(request)

}
