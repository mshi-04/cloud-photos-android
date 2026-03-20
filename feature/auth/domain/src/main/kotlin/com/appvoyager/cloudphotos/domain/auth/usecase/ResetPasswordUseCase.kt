package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.request.ResetPasswordRequest
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(request: ResetPasswordRequest): AuthResult<Unit> =
        repository.resetPassword(request)

}
