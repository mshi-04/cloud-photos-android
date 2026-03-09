package com.appvoyager.cloudphotos.domain.auth.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JwtTokenTest {

    @Test
    fun `of trims and creates token when non blank`() {
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
    }
    
}
