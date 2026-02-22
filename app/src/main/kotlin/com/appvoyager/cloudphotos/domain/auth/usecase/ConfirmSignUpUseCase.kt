package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmSignUpRequest
import javax.inject.Inject

class ConfirmSignUpUseCase @Inject constructor(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(request: ConfirmSignUpRequest): AuthResult<Unit> =
        repository.confirmSignUp(request)

}
