package com.appvoyager.cloudphotos.domain.auth.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PasswordTest {

    @Test
    fun `of returns password when length is minimum boundary`() {
        // Arrange
        val raw = "12345678"

        // Act
        val password = Password.of(raw)

        // Assert
        assertEquals(raw, password.value)
    }

    @Test
    fun `of throws when length below minimum`() {
        // Arrange
        val raw = "1234567"

        // Act
        val ex = assertThrows(IllegalArgumentException::class.java) {
            Password.of(raw)
        }

        // Assert
        assertEquals("Password must be at least 8 characters.", ex.message)
    }

    @Test
    fun `of returns password when length exceeds minimum`() {
        val raw = "12345678901234567890"
        val password = Password.of(raw)
        assertEquals(raw, password.value)
    }

    @Test
    fun `of throws when empty`() {
        val raw = ""
        val ex = assertThrows(IllegalArgumentException::class.java) {
            Password.of(raw)
        }
        assertEquals("Password must be at least 8 characters.", ex.message)
    }

}
