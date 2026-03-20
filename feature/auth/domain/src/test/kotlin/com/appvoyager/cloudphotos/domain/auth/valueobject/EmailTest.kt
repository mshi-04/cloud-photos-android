package com.appvoyager.cloudphotos.domain.auth.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EmailTest {

    @Test
    fun `of trims and creates email when valid format`() {
        // Arrange
        val raw = "  user@example.com  "

        // Act
        val email = Email.of(raw)

        // Assert
        assertEquals("user@example.com", email.value)
    }

    @Test
    fun `of throws when blank after trim`() {
        // Arrange
        val raw = "   "

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            Email.of(raw)
        }

        // Assert
        assertEquals("Email must not be blank.", ex.message)
    }

    @Test
    fun `of throws when invalid format without at mark`() {
        // Arrange
        val raw = "user.example.com"

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            Email.of(raw)
        }

        // Assert
        assertEquals("Invalid email format.", ex.message)
    }
}
