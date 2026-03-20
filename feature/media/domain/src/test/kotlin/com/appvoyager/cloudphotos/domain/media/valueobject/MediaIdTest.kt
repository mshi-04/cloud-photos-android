package com.appvoyager.cloudphotos.domain.media.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MediaIdTest {

    @Test
    fun `of returns MediaId with trimmed value when input is valid`() {
        // Arrange
        val rawId = " valid_id_123 "

        // Act
        val mediaId = MediaId.of(rawId)

        // Assert
        assertEquals("valid_id_123", mediaId.value)
    }

    @Test
    fun `of throws IllegalArgumentException when input is blank`() {
        // Arrange
        val rawId = "   "

        // Act
        val exception = assertThrows<IllegalArgumentException> {
            MediaId.of(rawId)
        }

        // Assert
        assertEquals("MediaId must not be blank.", exception.message)
    }

    @Test
    fun `of throws IllegalArgumentException when input is empty`() {
        // Arrange
        val rawId = ""

        // Act
        val exception = assertThrows<IllegalArgumentException> {
            MediaId.of(rawId)
        }

        // Assert
        assertEquals("MediaId must not be blank.", exception.message)
    }
}
