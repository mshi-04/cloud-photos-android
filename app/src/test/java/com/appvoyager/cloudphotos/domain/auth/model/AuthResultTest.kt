package com.appvoyager.cloudphotos.domain.auth.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AuthResultTest {

    @Test
    fun `getOrNull returns value for success`() {
        // Arrange
        val result: AuthResult<String> = AuthResult.Success("ok")

        // Act
        val value = result.getOrNull()

        // Assert
        assertEquals("ok", value)
    }

    @Test
    fun `getOrNull returns null for error`() {
        // Arrange
        val result: AuthResult<String> = AuthResult.Error(AuthError.Network("offline"))

        // Act
        val value = result.getOrNull()

        // Assert
        assertNull(value)
    }

    @Test
    fun `errorOrNull returns error for error result`() {
        // Arrange
        val error = AuthError.InvalidCredentials("invalid")
        val result: AuthResult<String> = AuthResult.Error(error)

        // Act
        val actual = result.errorOrNull()

        // Assert
        assertEquals(error, actual)
    }

    @Test
    fun `map transforms success value`() {
        // Arrange
        val result: AuthResult<Int> = AuthResult.Success(10)

        // Act
        val mapped = result.map { it * 2 }

        // Assert
        assertEquals(AuthResult.Success(20), mapped)
    }

    @Test
    fun `map keeps same error without transform`() {
        // Arrange
        val error = AuthError.Unknown("boom")
        val result: AuthResult<Int> = AuthResult.Error(error)

        // Act
        val mapped = result.map { it * 2 }

        // Assert
        assertEquals(AuthResult.Error(error), mapped)
    }

    @Test
    fun `flatMap transforms success into next result`() {
        // Arrange
        val result: AuthResult<Int> = AuthResult.Success(3)

        // Act
        val mapped = result.flatMap { AuthResult.Success(it.toString()) }

        // Assert
        assertEquals(AuthResult.Success("3"), mapped)
    }

    @Test
    fun `flatMap keeps error and does not execute transform`() {
        // Arrange
        val error = AuthError.Network("offline")
        val result: AuthResult<Int> = AuthResult.Error(error)

        // Act
        val mapped = result.flatMap { AuthResult.Success(it.toString()) }

        // Assert
        assertEquals(AuthResult.Error(error), mapped)
    }
}
