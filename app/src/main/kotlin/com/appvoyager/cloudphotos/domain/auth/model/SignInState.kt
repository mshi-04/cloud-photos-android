package com.appvoyager.cloudphotos.domain.auth.model

sealed class SignInState {

    data object SignedIn : SignInState()
    data class MFARequired(val step: SignInStep) : SignInState()
    data class NewPasswordRequired(val step: SignInStep) : SignInState()
    data class AdditionalStepRequired(val step: SignInStep) : SignInState()

}
