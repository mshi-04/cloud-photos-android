package com.appvoyager.cloudphotos.data.auth.testutil

import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.model.AuthState
import com.appvoyager.cloudphotos.domain.auth.model.AuthUser
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmSignUpRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignInRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignUpRequest
import com.appvoyager.cloudphotos.domain.auth.valueobject.ConfirmationCode
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.Password
import com.appvoyager.cloudphotos.domain.auth.valueobject.UserId

fun signUpRequestFixture(
    email: String = "fixture@example.com",
    password: String = "password123"
): SignUpRequest =
    SignUpRequest(
        email = Email.of(email),
        password = Password.of(password)
    )

fun signInRequestFixture(
    email: String = "fixture@example.com",
    password: String = "password123"
): SignInRequest =
    SignInRequest(
        email = Email.of(email),
        password = Password.of(password)
    )

fun confirmSignUpRequestFixture(
    email: String = "fixture@example.com",
    code: String = "123456"
): ConfirmSignUpRequest =
    ConfirmSignUpRequest(
        email = Email.of(email),
        code = ConfirmationCode.of(code)
    )

fun authUserFixture(
    userId: String = "user-123",
    email: String = "fixture@example.com"
): AuthUser =
    AuthUser(
        userId = UserId(userId),
        email = Email.of(email)
    )

fun authSessionFixture(
    state: AuthState = AuthState.SignedIn
): AuthSession = AuthSession(state)
