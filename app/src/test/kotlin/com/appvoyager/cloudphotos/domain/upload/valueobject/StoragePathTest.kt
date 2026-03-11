package com.appvoyager.cloudphotos.domain.upload.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StoragePathTest {

    @Test
    fun `of returns StoragePath with trimmed value when input is valid`() {
        // Arrange
        val raw = " photos/user123/image.jpg "

        // Act
        val storagePath = StoragePath.of(raw)

        // Assert
        assertEquals("photos/user123/image.jpg", storagePath.value)
    }

    @Test
    fun `of throws IllegalArgumentException when input is blank`() {
        // Arrange
        val raw = "   "

        // Act
        val exception = assertThrows<IllegalArgumentException> {
            StoragePath.of(raw)
        }

        // Assert
        assertEquals("StoragePath must not be blank.", exception.message)
    }

    @Test
    fun `of throws IllegalArgumentException when input is empty`() {
        // Arrange
        val raw = ""

        // Act
        val exception = assertThrows<IllegalArgumentException> {
            StoragePath.of(raw)
        }

        // Assert
        assertEquals("StoragePath must not be blank.", exception.message)
    }
}
