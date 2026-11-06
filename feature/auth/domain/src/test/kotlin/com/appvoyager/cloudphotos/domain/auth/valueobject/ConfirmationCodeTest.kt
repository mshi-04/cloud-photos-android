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

    @ParameterizedTest(name = "{displayName} [{0}]")
    @ValueSource(strings = ["１２３４５６", "١٢٣٤٥٦"])
    fun `of returns ASCII digits when six Unicode decimal digits`(raw: String) {
        // Act
        // Boundary: Accept Unicode decimal digits and normalize them to the ASCII code Cognito compares.
        val code = ConfirmationCode.of(raw)

        // Assert
        assertEquals("123456", code.value)
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @ValueSource(strings = ["¹²³⁴⁵⁶", "ⅠⅡⅢⅣⅤⅥ"])
    fun `of throws IllegalArgumentException when six numeric characters are not decimal digits`(raw: String) {
        // Act
        // Boundary: Reject numeric characters outside Char.isDigit.
        val ex = assertThrows<IllegalArgumentException> {
            ConfirmationCode.of(raw)
        }

        // Assert
        assertEquals("ConfirmationCode must contain only digits.", ex.message)
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
    fun `of throws IllegalArgumentException with length message when wrong length and contains non digits`() {
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

    @Test
    fun `of throws IllegalArgumentException with length message when three characters use surrogate pairs`() {
        // Arrange
        val raw = "𝟏𝟐𝟑"

        // Act
        // Boundary: Count characters by code point, not by UTF-16 unit.
        val ex = assertThrows<IllegalArgumentException> {
            ConfirmationCode.of(raw)
        }

        // Assert
        assertEquals("ConfirmationCode must be 6 characters.", ex.message)
    }
}
