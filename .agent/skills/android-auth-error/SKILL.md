---
name: android-auth-error
description: "Use when adding new AuthError types, modifying AuthErrorMapper, handling auth errors in DataSource, or mapping Cognito exceptions to domain errors"
---

# Auth Error Handling Guidelines

## Structure
```
domain/auth/model/AuthError.kt        ← sealed class (Domain層)
data/auth/util/AuthErrorMapper.kt     ← Cognitoの例外をAuthErrorにマップ (Data層)
```

## Rules
1. AuthError は Domain層。AWS/Amplify依存を一切持たない
2. AuthErrorMapper は `internal object`。Data層の外に公開しない
3. 新しいCognito例外は必ず AuthErrorMapper に追加する
4. `Unknown` はログ専用の逃げ道。causeをDomain層に漏らさない
5. アカウント列挙攻撃対策: `UserNotFoundException` は `InvalidCredentials` にマップする（ユーザーの存在を明かさない）

## AuthError sealed class
```kotlin
sealed class AuthError(open val message: String?) {
    data class CodeExpired(override val message: String? = null) : AuthError(message)
    data class CodeMismatch(override val message: String? = null) : AuthError(message)
    data class InvalidCredentials(override val message: String? = null) : AuthError(message)
    data class InvalidPassword(override val message: String? = null) : AuthError(message)
    data class Network(override val message: String? = null) : AuthError(message)
    data class TooManyRequests(override val message: String? = null) : AuthError(message)
    data class Unknown(override val message: String? = null) : AuthError(message)
    data class UserNotConfirmed(override val message: String? = null) : AuthError(message)
    data class UsernameAlreadyExists(override val message: String? = null) : AuthError(message)
}
```

## AuthErrorMapper Pattern
```kotlin
internal object AuthErrorMapper {

    fun map(throwable: Throwable): AuthError =
        when (throwable) {
            is NotAuthorizedException -> AuthError.InvalidCredentials(throwable.message)
            is SessionExpiredException -> AuthError.InvalidCredentials(throwable.message)
            is SignedOutException -> AuthError.InvalidCredentials(throwable.message)
            is IOException -> AuthError.Network(throwable.message)
            is ServiceException -> mapServiceException(throwable)
            is ValidationException -> mapValidationException(throwable)
            else -> AuthError.Unknown(throwable.message)
        }

    private fun mapServiceException(exception: ServiceException): AuthError =
        when (val cause = exception.cause) {
            is ExpiredCodeException -> AuthError.CodeExpired(cause.message)
            is CodeMismatchException -> AuthError.CodeMismatch(cause.message)
            is NotAuthorizedException -> AuthError.InvalidCredentials(cause.message)
            is InvalidPasswordException -> AuthError.InvalidPassword(cause.message)
            is LimitExceededException -> AuthError.TooManyRequests(cause.message)
            is TooManyRequestsException -> AuthError.TooManyRequests(cause.message)
            is UserNotConfirmedException -> AuthError.UserNotConfirmed(cause.message)
            is UsernameExistsException -> AuthError.UsernameAlreadyExists(cause.message)
            // UserNotFoundException は InvalidCredentials にマップ（アカウント列挙攻撃対策）
            else -> AuthError.Unknown(exception.message)
        }

    private fun mapValidationException(exception: ValidationException): AuthError {
        val message = exception.message ?: ""
        return when {
            message.contains("code", ignoreCase = true) -> AuthError.CodeMismatch(exception.message)
            else -> AuthError.Unknown(exception.message)
        }
    }
}
```