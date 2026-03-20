package com.appvoyager.cloudphotos.domain.media.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MediaCreatedAtTest {

    @Test
    fun `of returns MediaCreatedAt with correct value`() {
        // Arrange
        val expectedTimeMillis = 1678886400000L

        // Act
        val createdAt = MediaCreatedAt.of(expectedTimeMillis)

        // Assert
        assertEquals(expectedTimeMillis, createdAt.value)
    }

    @Test
    fun `of throws IllegalArgumentException when epochMillis is negative`() {
        // Arrange
        val negativeMillis = -1L

        // Act
        val exception = assertThrows<IllegalArgumentException> {
            MediaCreatedAt.of(negativeMillis)
        }

        // Assert
        assertEquals("createdAt must not be negative", exception.message)
    }

    @Test
    fun `of returns MediaCreatedAt with zero value`() {
        // Arrange
        val zeroMillis = 0L

        // Act
        val createdAt = MediaCreatedAt.of(zeroMillis)

        // Assert
        assertEquals(0L, createdAt.value)
    }
}
