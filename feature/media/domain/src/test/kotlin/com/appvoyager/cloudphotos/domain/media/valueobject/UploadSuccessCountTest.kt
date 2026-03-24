package com.appvoyager.cloudphotos.domain.media.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UploadSuccessCountTest {

    @Test
    fun `of returns UploadSuccessCount with given value when input is positive`() {
        // Arrange
        val raw = 5

        // Act
        val count = UploadSuccessCount.of(raw)

        // Assert
        assertEquals(raw, count.value)
    }

    @Test
    fun `of throws IllegalArgumentException when input is zero`() {
        // Act
        val exception = assertThrows<IllegalArgumentException> {
            UploadSuccessCount.of(0)
        }

        // Assert
        assertEquals("UploadSuccessCount must be positive.", exception.message)
    }

    @Test
    fun `of throws IllegalArgumentException when input is negative`() {
        // Act
        val exception = assertThrows<IllegalArgumentException> {
            UploadSuccessCount.of(-1)
        }

        // Assert
        assertEquals("UploadSuccessCount must be positive.", exception.message)
    }

}
