package com.appvoyager.cloudphotos.domain.auth.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserIdTest {

    @Test
    fun `constructor keeps value when non blank`() {
        // Arrange
        val raw = "user-id"

        // Act
        val userId = UserId(raw)

        // Assert
        assertEquals(raw, userId.value)
    }

    @Test
    fun `constructor throws when blank`() {
        // Arrange
        val raw = ""

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            UserId(raw)
        }

        // Assert
        assertEquals("UserId must not be blank.", ex.message)
    }

    @Test
    fun `constructor throws when whitespace only`() {
        // Arrange
        val raw = "   "

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            UserId(raw)
        }

        // Assert
        val raw = "   "
        val ex = assertThrows<IllegalArgumentException> {
            UserId(raw)
        }
        assertEquals("UserId must not be blank.", ex.message)
    }

}
