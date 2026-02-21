package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor (
    private val repository: AuthRepository
) {

    suspend operator fun invoke(): AuthResult<Unit> =
        repository.signOut()

}
