package com.appvoyager.cloudphotos.domain.auth.util

import com.amplifyframework.auth.result.step.AuthSignInStep
import com.appvoyager.cloudphotos.domain.auth.model.SignInStep

object AuthSignInStepMapper {

    fun mapSignInStep(signInStep: AuthSignInStep): SignInStep =
        when (signInStep) {
            AuthSignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE -> SignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE

            AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE -> SignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE

            AuthSignInStep.CONFIRM_SIGN_IN_WITH_TOTP_CODE -> SignInStep.CONFIRM_SIGN_IN_WITH_TOTP_CODE

            AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION -> SignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION

            AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP -> SignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP

            AuthSignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD -> SignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD

            AuthSignInStep.DONE -> SignInStep.DONE

            else -> SignInStep.UNKNOWN
        }

}
