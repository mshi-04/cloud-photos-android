package com.appvoyager.cloudphotos.domain.auth.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ConfirmationCodeTest {

    @Test
    fun `of returns code when six digits`() {
        // Arrange
        val raw = "123456"

        // Act
        val code = ConfirmationCode.of(raw)

        // Assert
        assertEquals(raw, code.value)
    }

    @Test
    fun `of trims and returns code when padded by whitespace`() {
        // Arrange
        val raw = " 123456 "

        // Act
        val code = ConfirmationCode.of(raw)

        // Assert
        assertEquals("123456", code.value)
    }

    @Test
    fun `of throws when blank after trim`() {
        // Arrange
        val raw = "   "

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            ConfirmationCode.of(raw)
        }

        // Assert
        assertEquals("ConfirmationCode must not be blank.", ex.message)
    }

    @Test
    fun `of throws when length less than six`() {
        // Arrange
        val raw = "12345"

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            ConfirmationCode.of(raw)
        }

        // Assert
        assertEquals("ConfirmationCode must be 6 digits.", ex.message)
    }

    @Test
    fun `of throws when length greater than six`() {
        // Arrange
        val raw = "1234567"

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            ConfirmationCode.of(raw)
        }

        // Assert
        assertEquals("ConfirmationCode must be 6 digits.", ex.message)
    }

    @Test
    fun `of throws when contains non digits`() {
        // Arrange
        val raw = "12A456"

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            ConfirmationCode.of(raw)
        }

        // Assert
        assertEquals("ConfirmationCode must be 6 digits.", ex.message)
    }
}
