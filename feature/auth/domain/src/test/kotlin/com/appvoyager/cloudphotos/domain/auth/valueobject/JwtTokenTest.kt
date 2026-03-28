package com.appvoyager.cloudphotos.domain.auth.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JwtTokenTest {

    @Test
    fun `of returns JwtToken with trimmed value when input is non blank`() {
        // Arrange
        val raw = "  token-value  "

        // Act
        val token = JwtToken.of(raw)

        // Assert
        assertEquals("token-value", token.value)
    }

    @Test
    fun `of throws when blank after trim`() {
        // Arrange
        val raw = "\n\t "

        // Act & Assert
        val ex = assertThrows<IllegalArgumentException> {
            JwtToken.of(raw)
        }
        assertEquals("Token must not be blank.", ex.message)
    }
}
