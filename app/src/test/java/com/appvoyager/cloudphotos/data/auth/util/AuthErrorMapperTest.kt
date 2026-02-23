package com.appvoyager.cloudphotos.data.auth.util

import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.exceptions.ValidationException
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException

class AuthErrorMapperTest {

    @Test
    fun `map returns InvalidCredentials for session expired exception`() {
        // Arrange
        val throwable = SessionExpiredException("session expired", "retry")

        // Act
        val actual = AuthErrorMapper.map(throwable)

        // Assert
        assertEquals(AuthError.InvalidCredentials("session expired"), actual)
    }

    @Test
    fun `map returns InvalidCredentials for signed out exception`() {
        // Arrange
        val throwable = SignedOutException("signed out", "retry")

        // Act
        val actual = AuthErrorMapper.map(throwable)

        // Assert
        assertEquals(AuthError.InvalidCredentials("signed out"), actual)
    }

    @Test
    fun `map returns CodeMismatch when validation message contains code`() {
        // Arrange
        val throwable = ValidationException("Code is invalid", "retry")

        // Act
        val actual = AuthErrorMapper.map(throwable)

        // Assert
        assertEquals(AuthError.CodeMismatch("Code is invalid"), actual)
    }

    @Test
    fun `map returns Unknown when validation message does not contain code`() {
        // Arrange
        val throwable = ValidationException("email is malformed", "retry")

        // Act
        val actual = AuthErrorMapper.map(throwable)

        // Assert
        assertEquals(AuthError.Unknown("email is malformed"), actual)
    }

    @Test
    fun `map returns Unknown for service exception with unmapped cause`() {
        // Arrange
        val throwable = ServiceException("service unavailable", "retry", IllegalStateException("boom"))

        // Act
        val actual = AuthErrorMapper.map(throwable)

        // Assert
        assertEquals(AuthError.Unknown("service unavailable"), actual)
    }

    @Test
    fun `map returns Network for io exception`() {
        // Arrange
        val throwable = IOException("timeout")

        // Act
        val actual = AuthErrorMapper.map(throwable)

        // Assert
        assertEquals(AuthError.Network("timeout"), actual)
    }

    @Test
    fun `map returns Unknown for unexpected throwable`() {
        // Arrange
        val throwable = IllegalArgumentException("unexpected")

        // Act
        val actual = AuthErrorMapper.map(throwable)

        // Assert
        assertEquals(AuthError.Unknown("unexpected"), actual)
    }
}
