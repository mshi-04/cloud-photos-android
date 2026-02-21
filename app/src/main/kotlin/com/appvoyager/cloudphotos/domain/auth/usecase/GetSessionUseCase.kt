package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository

class GetSessionUseCase(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(): AuthResult<AuthSession> =
        repository.getSession()

}
