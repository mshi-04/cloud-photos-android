package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmResetPasswordRequest
import javax.inject.Inject

class ConfirmResetPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(request: ConfirmResetPasswordRequest): AuthResult<Unit> =
        repository.confirmResetPassword(request)

}
