package com.appvoyager.cloudphotos.domain.auth.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

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
        val ex = assertThrows(IllegalArgumentException::class.java) {
            UserId(raw)
        }

        // Assert
        assertEquals("UserId must not be blank.", ex.message)
    }
}
