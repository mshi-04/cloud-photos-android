package com.appvoyager.cloudphotos.domain.media.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MediaUploadedAtTest {

    @Test
    fun `of returns MediaUploadedAt with correct value`() {
        // Arrange
        val expectedTimeMillis = 1678886400000L

        // Act
        val uploadedAt = MediaUploadedAt.of(expectedTimeMillis)

        // Assert
        assertEquals(expectedTimeMillis, uploadedAt.value)
    }

    @Test
    fun `of throws IllegalArgumentException when epochMillis is negative`() {
        // Arrange
        val negativeMillis = -1L

        // Act
        val exception = assertThrows<IllegalArgumentException> {
            MediaUploadedAt.of(negativeMillis)
        }

        // Assert
        assertEquals("uploadedAt must not be negative", exception.message)
    }

    @Test
    fun `of returns MediaUploadedAt with zero value`() {
        // Arrange
        val zeroMillis = 0L

        // Act
        val uploadedAt = MediaUploadedAt.of(zeroMillis)

        // Assert
        assertEquals(0L, uploadedAt.value)
    }
}
