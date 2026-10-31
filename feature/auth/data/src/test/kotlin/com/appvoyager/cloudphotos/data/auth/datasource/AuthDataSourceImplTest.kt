package com.appvoyager.cloudphotos.data.auth.datasource

import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserNotFoundException
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.appvoyager.cloudphotos.data.auth.testutil.confirmSignUpRequestFixture
import com.appvoyager.cloudphotos.data.auth.testutil.signInRequestFixture
import com.appvoyager.cloudphotos.data.auth.testutil.signUpRequestFixture
import com.appvoyager.cloudphotos.data.fcm.DeviceTokenDataSource
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.AuthSession
import com.appvoyager.cloudphotos.domain.auth.model.AuthState
import com.appvoyager.cloudphotos.domain.auth.model.AuthUser
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.model.SignInStep
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResendSignUpCodeRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.valueobject.ConfirmationCode
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.Password
import com.appvoyager.cloudphotos.domain.auth.valueobject.UserId
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import com.amplifyframework.auth.AuthSession as SdkAuthSession
import com.amplifyframework.auth.AuthUser as SdkAuthUser

class AuthDataSourceImplTest {

    private val auth = mockk<AmplifyAuthFacade>()
    private val deviceTokenDataSource = mockk<DeviceTokenDataSource>()
    private lateinit var dataSource: AuthDataSourceImpl

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        dataSource = AuthDataSourceImpl(deviceTokenDataSource, auth)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "signUp", "confirmSignUp", "signIn", "fetchCurrentUser", "getSession",
            "resendSignUpCode", "resetPassword", "confirmResetPassword"
        ]
    )
    fun `operation rethrows CancellationException when SDK call is cancelled`(operation: String) = runTest {
        // Arrange
        val cancellation = CancellationException("Cancelled")
        stubFailure(operation, cancellation)

        // Act
        // Coroutine: Every authentication entry point preserves the original cancellation.
        val actual = assertThrows<CancellationException> { invokeOperation(operation) }

        // Assert
        assertSame(cancellation, actual)
        verifyOperation(operation)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "signUp", "confirmSignUp", "signIn", "fetchCurrentUser", "getSession",
            "resendSignUpCode", "resetPassword", "confirmResetPassword"
        ]
    )
    fun `operation returns Network when SDK call fails with IOException`(operation: String) = runTest {
        // Arrange
        stubFailure(operation, IOException("Offline"))

        // Act
        // Error: Ordinary failures still use the domain error contract after sharing the wrapper.
        val actual = invokeOperation(operation)

        // Assert
        assertEquals(AuthResult.Error(AuthError.Network("Offline")), actual)
        verifyOperation(operation)
    }

    @ParameterizedTest
    @ValueSource(strings = ["signUp", "confirmSignUp", "resendSignUpCode", "resetPassword", "confirmResetPassword"])
    fun `operation returns Success when SDK command succeeds`(operation: String) = runTest {
        // Arrange
        coEvery { auth.signUp(any(), any(), any()) } just runs
        coEvery { auth.confirmSignUp(any(), any()) } just runs
        coEvery { auth.resendSignUpCode(any()) } just runs
        coEvery { auth.resetPassword(any()) } just runs
        coEvery { auth.confirmResetPassword(any(), any(), any()) } just runs

        // Act
        // Normal: Command results remain Unit and domain values are passed to the SDK boundary.
        val actual = invokeOperation(operation)

        // Assert
        assertEquals(AuthResult.Success(Unit), actual)
        verifyOperation(operation)
    }

    @Test
    fun `signUp calls SDK with email attribute when request is valid`() = runTest {
        // Arrange
        val request = signUpRequestFixture()
        val options = slot<AuthSignUpOptions>()
        coEvery { auth.signUp(request.email.value, request.password.value, capture(options)) } just runs

        // Act
        // Interaction: The provider receives the account email as an explicit user attribute.
        val actual = dataSource.signUp(request)

        // Assert
        assertEquals(AuthResult.Success(Unit), actual)
        assertEquals(AuthUserAttributeKey.email(), options.captured.userAttributes.single().key)
        assertEquals(request.email.value, options.captured.userAttributes.single().value)
        coVerify(exactly = 1) { auth.signUp(request.email.value, request.password.value, any()) }
    }

    @Test
    fun `signIn returns SignedIn when SDK reports signed in`() = runTest {
        // Arrange
        val sdkResult = mockk<AuthSignInResult>()
        every { sdkResult.isSignedIn } returns true
        coEvery { auth.signIn(any(), any()) } returns sdkResult

        // Act
        // Normal: Signed-in sessions do not require reading an additional authentication step.
        val actual = dataSource.signIn(signInRequestFixture())

        // Assert
        assertEquals(AuthResult.Success(SignInState.SignedIn), actual)
        verifyOperation("signIn")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE", "CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE",
            "CONFIRM_SIGN_IN_WITH_TOTP_CODE", "CONTINUE_SIGN_IN_WITH_MFA_SELECTION",
            "CONTINUE_SIGN_IN_WITH_TOTP_SETUP"
        ]
    )
    fun `signIn returns MFARequired when SDK requests MFA`(stepName: String) = runTest {
        // Arrange
        stubSignInStep(AuthSignInStep.valueOf(stepName))

        // Act
        // State: Existing MFA steps keep their domain step and required-authentication category.
        val actual = dataSource.signIn(signInRequestFixture())

        // Assert
        assertEquals(AuthResult.Success(SignInState.MFARequired(SignInStep.valueOf(stepName))), actual)
        verifyOperation("signIn")
    }

    @Test
    fun `signIn returns NewPasswordRequired when SDK requests new password`() = runTest {
        // Arrange
        stubSignInStep(AuthSignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD)

        // Act
        // State: New-password challenges remain distinct from MFA and ordinary failures.
        val actual = dataSource.signIn(signInRequestFixture())

        // Assert
        assertEquals(
            AuthResult.Success(SignInState.NewPasswordRequired(SignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD)),
            actual
        )
        verifyOperation("signIn")
    }

    @Test
    fun `signIn returns SignedIn when SDK next step is DONE`() = runTest {
        // Arrange
        stubSignInStep(AuthSignInStep.DONE)

        // Act
        // State: DONE remains a completed sign-in even when the SDK signed-in flag is false.
        val actual = dataSource.signIn(signInRequestFixture())

        // Assert
        assertEquals(AuthResult.Success(SignInState.SignedIn), actual)
        verifyOperation("signIn")
    }

    @Test
    fun `signIn returns AdditionalStepRequired when SDK next step has no domain mapping`() = runTest {
        // Arrange
        stubSignInStep(AuthSignInStep.CONFIRM_SIGN_UP)

        // Act
        // State: Unmapped provider steps keep the existing additional-authentication fallback.
        val actual = dataSource.signIn(signInRequestFixture())

        // Assert
        assertEquals(AuthResult.Success(SignInState.AdditionalStepRequired(SignInStep.UNKNOWN)), actual)
        verifyOperation("signIn")
    }

    @Test
    fun `signIn returns InvalidCredentials when SDK reports missing user`() = runTest {
        // Arrange
        val cause = UserNotFoundException { message = "User not found" }
        coEvery { auth.signIn(any(), any()) } throws ServiceException("Rejected", "Retry", cause)

        // Act
        // Error: Provider user-existence information keeps the shared invalid-credentials category.
        val actual = dataSource.signIn(signInRequestFixture())

        // Assert
        assertEquals(AuthResult.Error(AuthError.InvalidCredentials("User not found")), actual)
        verifyOperation("signIn")
    }

    @Test
    fun `fetchCurrentUser returns domain user when SDK user and email are valid`() = runTest {
        // Arrange
        stubCurrentUser()
        coEvery { auth.fetchUserAttributes() } returns listOf(
            AuthUserAttribute(AuthUserAttributeKey.email(), "fixture@example.com")
        )

        // Act
        // Normal: SDK identifiers and attributes are converted into validated domain values.
        val actual = dataSource.fetchCurrentUser()

        // Assert
        assertEquals(
            AuthResult.Success(AuthUser(UserId.of("user-123"), Email.of("fixture@example.com"))),
            actual
        )
        coVerifyOrder {
            auth.getCurrentUser()
            auth.fetchUserAttributes()
        }
    }

    @Test
    fun `fetchCurrentUser rethrows CancellationException when attribute lookup is cancelled`() = runTest {
        // Arrange
        stubCurrentUser()
        val cancellation = CancellationException("Attribute lookup cancelled")
        coEvery { auth.fetchUserAttributes() } throws cancellation

        // Act
        // Coroutine: The second SDK wait must also escape the shared result-mapping boundary.
        val actual = assertThrows<CancellationException> { dataSource.fetchCurrentUser() }

        // Assert
        assertSame(cancellation, actual)
        coVerify(exactly = 1) { auth.fetchUserAttributes() }
    }

    @Test
    fun `fetchCurrentUser returns Unknown when email attribute is missing`() = runTest {
        // Arrange
        stubCurrentUser()
        coEvery { auth.fetchUserAttributes() } returns emptyList()

        // Act
        // Error: Invalid provider data is mapped without exposing a provider model to callers.
        val actual = dataSource.fetchCurrentUser()

        // Assert
        assertEquals(AuthResult.Error(AuthError.Unknown("Email attribute not found for current user")), actual)
        coVerify(exactly = 1) { auth.fetchUserAttributes() }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `getSession returns domain session when SDK session is available`(signedIn: Boolean) = runTest {
        // Arrange
        val session = mockk<SdkAuthSession>()
        every { session.isSignedIn } returns signedIn
        coEvery { auth.fetchAuthSession() } returns session

        // Act
        // State: Both authenticated and guest sessions retain their domain state.
        val actual = dataSource.getSession()

        // Assert
        val expected = if (signedIn) AuthState.SignedIn else AuthState.Guest
        assertEquals(AuthResult.Success(AuthSession(expected)), actual)
        verifyOperation("getSession")
    }

    private fun stubCurrentUser() {
        val user = mockk<SdkAuthUser>()
        every { user.userId } returns "user-123"
        coEvery { auth.getCurrentUser() } returns user
    }

    private fun stubSignInStep(step: AuthSignInStep) {
        val result = mockk<AuthSignInResult>()
        every { result.isSignedIn } returns false
        every { result.nextStep.signInStep } returns step
        coEvery { auth.signIn(any(), any()) } returns result
    }

    private fun stubFailure(operation: String, failure: Throwable) {
        when (operation) {
            "signUp" -> coEvery { auth.signUp(any(), any(), any()) } throws failure
            "confirmSignUp" -> coEvery { auth.confirmSignUp(any(), any()) } throws failure
            "signIn" -> coEvery { auth.signIn(any(), any()) } throws failure
            "fetchCurrentUser" -> coEvery { auth.getCurrentUser() } throws failure
            "getSession" -> coEvery { auth.fetchAuthSession() } throws failure
            "resendSignUpCode" -> coEvery { auth.resendSignUpCode(any()) } throws failure
            "resetPassword" -> coEvery { auth.resetPassword(any()) } throws failure
            "confirmResetPassword" -> coEvery { auth.confirmResetPassword(any(), any(), any()) } throws failure
            else -> error("Unknown operation: $operation")
        }
    }

    private suspend fun invokeOperation(operation: String): AuthResult<*> = when (operation) {
        "signUp" -> dataSource.signUp(signUpRequestFixture())
        "confirmSignUp" -> dataSource.confirmSignUp(confirmSignUpRequestFixture())
        "signIn" -> dataSource.signIn(signInRequestFixture())
        "fetchCurrentUser" -> dataSource.fetchCurrentUser()
        "getSession" -> dataSource.getSession()
        "resendSignUpCode" -> dataSource.resendSignUpCode(ResendSignUpCodeRequest(Email.of("fixture@example.com")))
        "resetPassword" -> dataSource.resetPassword(ResetPasswordRequest(Email.of("fixture@example.com")))
        "confirmResetPassword" -> dataSource.confirmResetPassword(
            ConfirmResetPasswordRequest(
                Email.of("fixture@example.com"),
                ConfirmationCode.of("123456"),
                Password.of("password123")
            )
        )
        else -> error("Unknown operation: $operation")
    }

    private fun verifyOperation(operation: String) {
        when (operation) {
            "signUp" -> coVerify(exactly = 1) { auth.signUp("fixture@example.com", "password123", any()) }
            "confirmSignUp" -> coVerify(exactly = 1) { auth.confirmSignUp("fixture@example.com", "123456") }
            "signIn" -> coVerify(exactly = 1) { auth.signIn("fixture@example.com", "password123") }
            "fetchCurrentUser" -> coVerify(exactly = 1) { auth.getCurrentUser() }
            "getSession" -> coVerify(exactly = 1) { auth.fetchAuthSession() }
            "resendSignUpCode" -> coVerify(exactly = 1) { auth.resendSignUpCode("fixture@example.com") }
            "resetPassword" -> coVerify(exactly = 1) { auth.resetPassword("fixture@example.com") }
            "confirmResetPassword" -> coVerify(exactly = 1) {
                auth.confirmResetPassword("fixture@example.com", "password123", "123456")
            }
            else -> error("Unknown operation: $operation")
        }
    }
}
