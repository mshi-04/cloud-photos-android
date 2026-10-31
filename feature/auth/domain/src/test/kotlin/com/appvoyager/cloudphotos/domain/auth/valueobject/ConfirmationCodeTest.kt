package com.appvoyager.cloudphotos.domain.auth.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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
    fun `of returns trimmed code when padded by whitespace`() {
        // Arrange
        val raw = " 123456 "

        // Act
        val code = ConfirmationCode.of(raw)

        // Assert
        assertEquals("123456", code.value)
    }

    @ParameterizedTest
    @ValueSource(strings = ["１２３４５６", "١٢٣٤٥٦"])
    fun `of returns code when six Unicode digits`(raw: String) {
        // Arrange & Act
        // Boundary: Preserve the Unicode digits accepted by Char.isDigit.
        val code = ConfirmationCode.of(raw)

        // Assert
        assertEquals(raw, code.value)
    }

    @Test
    fun `of throws IllegalArgumentException when blank after trim`() {
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
    fun `of throws IllegalArgumentException when length less than six`() {
        // Arrange
        val raw = "12345"

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            ConfirmationCode.of(raw)
        }

        // Assert
        assertEquals("ConfirmationCode must be 6 characters.", ex.message)
    }

    @Test
    fun `of throws IllegalArgumentException when length greater than six`() {
        // Arrange
        val raw = "1234567"

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            ConfirmationCode.of(raw)
        }

        // Assert
        assertEquals("ConfirmationCode must be 6 characters.", ex.message)
    }

    @Test
    fun `of throws IllegalArgumentException when contains non digits`() {
        // Arrange
        val raw = "12A456"

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            ConfirmationCode.of(raw)
        }

        // Assert
        assertEquals("ConfirmationCode must contain only digits.", ex.message)
    }

    @Test
    fun `of throws length error when wrong length and contains non digits`() {
        // Arrange
        val raw = "12A45"

        // Act
        // Error: Report the length constraint before checking the character type.
        val ex = assertThrows<IllegalArgumentException> {
            ConfirmationCode.of(raw)
        }

        // Assert
        assertEquals("ConfirmationCode must be 6 characters.", ex.message)
    }
}
