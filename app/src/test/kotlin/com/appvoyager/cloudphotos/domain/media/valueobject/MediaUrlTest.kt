package com.appvoyager.cloudphotos.domain.media.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MediaUrlTest {

    @Test
    fun `of returns MediaUrl with trimmed value when input is a valid valid`() {
        // Arrange
        val rawUrl = " https://example.com/image.jpg "

        // Act
        val mediaUrl = MediaUrl.of(rawUrl)

        // Assert
        assertEquals("https://example.com/image.jpg", mediaUrl.value)
    }

    @Test
    fun `of throws IllegalArgumentException when input is blank`() {
        // Arrange
        val rawUrl = "   "

        // Act
        val exception = assertThrows<IllegalArgumentException> {
            MediaUrl.of(rawUrl)
        }

        // Assert
        assertEquals("MediaUrl must not be blank.", exception.message)
    }

    @Test
    fun `of throws IllegalArgumentException when input is empty`() {
        // Arrange
        val rawUrl = ""

        // Act
        val exception = assertThrows<IllegalArgumentException> {
            MediaUrl.of(rawUrl)
        }

        // Assert
        assertEquals("MediaUrl must not be blank.", exception.message)
    }

    @Test
    fun `of throws IllegalArgumentException when input is not a valid URL`() {
        // Arrange
        val rawUrl = "hppt://example.com"

        // Act
        val exception = assertThrows<IllegalArgumentException> {
            MediaUrl.of(rawUrl)
        }

        // Assert
        assertEquals("MediaUrl must be a valid URL/URI.", exception.message)
    }

    @Test
    fun `ofOrNull returns MediaUrl when input is a valid URL`() {
        // Arrange
        val rawUrl = " https://example.com/image.jpg "

        // Act
        val mediaUrl = MediaUrl.ofOrNull(rawUrl)

        // Assert
        assertEquals("https://example.com/image.jpg", mediaUrl?.value)
    }

    @Test
    fun `ofOrNull returns null when input is null`() {
        // Arrange
        val rawUrl: String? = null

        // Act
        val mediaUrl = MediaUrl.ofOrNull(rawUrl)

        // Assert
        assertNull(mediaUrl)
    }

    @Test
    fun `ofOrNull returns null when input is blank`() {
        // Arrange
        val rawUrl = "   "

        // Act
        val mediaUrl = MediaUrl.ofOrNull(rawUrl)

        // Assert
        assertNull(mediaUrl)
    }

    @Test
    fun `ofOrNull returns null when input is empty`() {
        // Arrange
        val rawUrl = ""

        // Act
        val mediaUrl = MediaUrl.ofOrNull(rawUrl)

        // Assert
        assertNull(mediaUrl)
    }

    @Test
    fun `ofOrNull returns null when input is not a valid URL`() {
        // Arrange
        val rawUrl = "hppt://example.com"

        // Act
        val mediaUrl = MediaUrl.ofOrNull(rawUrl)

        // Assert
        assertNull(mediaUrl)
    }
}
