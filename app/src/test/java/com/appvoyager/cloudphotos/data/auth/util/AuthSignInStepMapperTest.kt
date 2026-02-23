package com.appvoyager.cloudphotos.data.auth.util

import com.amplifyframework.auth.result.step.AuthSignInStep
import com.appvoyager.cloudphotos.domain.auth.model.SignInStep
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthSignInStepMapperTest {

    @Test
    fun `mapSignInStep maps known steps to corresponding domain step`() {
        // Arrange
        val cases = listOf(
            AuthSignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE to SignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE,
            AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE to SignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE,
            AuthSignInStep.CONFIRM_SIGN_IN_WITH_TOTP_CODE to SignInStep.CONFIRM_SIGN_IN_WITH_TOTP_CODE,
            AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION to SignInStep.CONTINUE_SIGN_IN_WITH_MFA_SELECTION,
            AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP to SignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP,
            AuthSignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD to SignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD,
            AuthSignInStep.DONE to SignInStep.DONE,
        )

        // Act & Assert
        cases.forEach { (source, expected) ->
            assertEquals(expected, AuthSignInStepMapper.mapSignInStep(source))
        }
    }

    @Test
    fun `mapSignInStep returns UNKNOWN for unmapped step`() {
        // Arrange
        val source = AuthSignInStep.RESET_PASSWORD

        // Act
        val actual = AuthSignInStepMapper.mapSignInStep(source)

        // Assert
        assertEquals(SignInStep.UNKNOWN, actual)
    }
}
