package com.appvoyager.cloudphotos.domain.auth.testutil

import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.model.AuthState
import com.appvoyager.cloudphotos.domain.auth.model.AuthUser
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.model.SignInStep
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmSignUpRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignInRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignUpRequest
import com.appvoyager.cloudphotos.domain.auth.valueobject.ConfirmationCode
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.Password
import com.appvoyager.cloudphotos.domain.auth.valueobject.UserId

fun validEmail(raw: String = "user@example.com"): Email = Email.of(raw)
fun validPassword(raw: String = "password1"): Password = Password.of(raw)
fun validCode(raw: String = "123456"): ConfirmationCode = ConfirmationCode.of(raw)

fun signInRequest(
    email: Email = validEmail(),
    password: Password = validPassword()
): SignInRequest = SignInRequest(email = email, password = password)

fun signUpRequest(
    email: Email = validEmail(),
    password: Password = validPassword()
): SignUpRequest = SignUpRequest(email = email, password = password)

fun confirmSignUpRequest(
    email: Email = validEmail(),
    code: ConfirmationCode = validCode()
): ConfirmSignUpRequest = ConfirmSignUpRequest(email = email, code = code)

fun authUser(
    userId: UserId = UserId.of("user-1"),
    email: Email? = validEmail()
): AuthUser = AuthUser(userId = userId, email = email)

fun signedInSession(): AuthSession = AuthSession(state = AuthState.SignedIn)
fun guestSession(): AuthSession = AuthSession(state = AuthState.Guest)
fun mfaRequiredState(step: SignInStep = SignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE): SignInState =
    SignInState.MFARequired(step)
