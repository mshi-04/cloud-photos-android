package com.appvoyager.cloudphotos.domain.auth.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserIdTest {

    @Test
    fun `of keeps value when non blank`() {
        // Arrange
        val raw = "user-id"

        // Act
        val userId = UserId.of(raw)

        // Assert
        assertEquals(raw, userId.value)
    }

    @Test
    fun `of trims whitespace from input`() {
        // Arrange
        val raw = "  user-id  "

        // Act
        val userId = UserId.of(raw)

        // Assert
        assertEquals("user-id", userId.value)
    }

    @Test
    fun `of throws when blank`() {
        // Arrange
        val raw = ""

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            UserId.of(raw)
        }

        // Assert
        assertEquals("UserId must not be blank.", ex.message)
    }

    @Test
    fun `of throws when whitespace only`() {
        // Arrange
        val raw = "   "

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            UserId.of(raw)
        }

        // Assert
        assertEquals("UserId must not be blank.", ex.message)
    }

}