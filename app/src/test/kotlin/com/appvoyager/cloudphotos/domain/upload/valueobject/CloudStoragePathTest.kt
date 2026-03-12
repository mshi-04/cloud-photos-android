package com.appvoyager.cloudphotos.domain.upload.valueobject

import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CloudStoragePathTest {

    @Test
    fun `of returns CloudStoragePath with trimmed value when input is valid`() {
        // Arrange
        val raw = " photos/user123/image.jpg "

        // Act
        val storagePath = CloudStoragePath.of(raw)

        // Assert
        assertEquals("photos/user123/image.jpg", storagePath.value)
    }

    @Test
    fun `of throws IllegalArgumentException when input is blank`() {
        // Arrange
        val raw = "   "

        // Act
        val exception = assertThrows<IllegalArgumentException> {
            CloudStoragePath.of(raw)
        }

        // Assert
        assertEquals("CloudStoragePath must not be blank.", exception.message)
    }

    @Test
    fun `of throws IllegalArgumentException when input is empty`() {
        // Arrange
        val raw = ""

        // Act
        val exception = assertThrows<IllegalArgumentException> {
            CloudStoragePath.of(raw)
        }

        // Assert
        assertEquals("CloudStoragePath must not be blank.", exception.message)
    }
}
