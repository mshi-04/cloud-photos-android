package com.appvoyager.cloudphotos.domain.settings.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GridColumnCountTest {

    @Test
    fun `of creates GridColumnCount when value is within range`() {
        // Arrange
        val raw = 3

        // Act
        val count = GridColumnCount.of(raw)

        // Assert
        assertEquals(3, count.value)
    }

    @Test
    fun `of creates GridColumnCount when value is MIN`() {
        // Arrange
        val raw = GridColumnCount.MIN

        // Act
        val count = GridColumnCount.of(raw)

        // Assert
        assertEquals(GridColumnCount.MIN, count.value)
    }

    @Test
    fun `of creates GridColumnCount when value is MAX`() {
        // Arrange
        val raw = GridColumnCount.MAX

        // Act
        val count = GridColumnCount.of(raw)

        // Assert
        assertEquals(GridColumnCount.MAX, count.value)
    }

    @Test
    fun `of throws when value is below MIN`() {
        // Arrange
        val raw = GridColumnCount.MIN - 1

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            GridColumnCount.of(raw)
        }
    }

    @Test
    fun `of throws when value is above MAX`() {
        // Arrange
        val raw = GridColumnCount.MAX + 1

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            GridColumnCount.of(raw)
        }
    }

    @Test
    fun `of throws when value is zero`() {
        // Arrange
        val raw = 0

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            GridColumnCount.of(raw)
        }
    }

    @Test
    fun `of throws when value is negative`() {
        // Arrange
        val raw = -1

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            GridColumnCount.of(raw)
        }
    }
}