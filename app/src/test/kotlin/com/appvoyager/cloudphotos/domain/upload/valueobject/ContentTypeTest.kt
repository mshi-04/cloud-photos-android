package com.appvoyager.cloudphotos.domain.upload.valueobject

import com.appvoyager.cloudphotos.domain.media.valueobject.ContentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ContentTypeTest {

    @Test
    fun `of returns ContentType with trimmed value when input is image type`() {
        // Arrange
        val raw = " image/jpeg "

        // Act
        val contentType = ContentType.of(raw)

        // Assert
        assertEquals("image/jpeg", contentType.value)
    }

    @Test
    fun `of returns ContentType when input is video type`() {
        // Arrange
        val raw = "video/mp4"

        // Act
        val contentType = ContentType.of(raw)

        // Assert
        assertEquals("video/mp4", contentType.value)
    }

    @Test
    fun `of throws IllegalArgumentException when input is blank`() {
        // Arrange
        val raw = "   "

        // Act
        val exception = assertThrows<IllegalArgumentException> {
            ContentType.of(raw)
        }

        // Assert
        assertEquals("ContentType must not be blank.", exception.message)
    }

    @Test
    fun `of throws IllegalArgumentException when input is empty`() {
        // Arrange
        val raw = ""

        // Act
        val exception = assertThrows<IllegalArgumentException> {
            ContentType.of(raw)
        }

        // Assert
        assertEquals("ContentType must not be blank.", exception.message)
    }

    @Test
    fun `of throws IllegalArgumentException when input is not image or video type`() {
        // Arrange
        val raw = "application/pdf"

        // Act
        val exception = assertThrows<IllegalArgumentException> {
            ContentType.of(raw)
        }

        // Assert
        assertEquals("ContentType must be an image or video type.", exception.message)
    }
}
