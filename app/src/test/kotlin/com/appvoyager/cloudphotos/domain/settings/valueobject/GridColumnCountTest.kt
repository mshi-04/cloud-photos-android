package com.appvoyager.cloudphotos.domain.settings.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GridColumnCountTest {

    @Test
    fun `of creates GridColumnCount when value is positive`() {
        // Arrange
        val raw = 3

        // Act
        val count = GridColumnCount.of(raw)

        // Assert
        assertEquals(3, count.value)
    }

    @Test
    fun `of creates GridColumnCount when value is 1`() {
        // Arrange
        val raw = 1

        // Act
        val count = GridColumnCount.of(raw)

        // Assert
        assertEquals(1, count.value)
    }

    @Test
    fun `of throws when value is zero`() {
        // Arrange
        val raw = 0

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            GridColumnCount.of(raw)
        }

        // Assert
        assertEquals("GridColumnCount must be greater than 0.", ex.message)
    }

    @Test
    fun `of throws when value is negative`() {
        // Arrange
        val raw = -1

        // Act
        val ex = assertThrows<IllegalArgumentException> {
            GridColumnCount.of(raw)
        }

        // Assert
        assertEquals("GridColumnCount must be greater than 0.", ex.message)
    }
}