package com.appvoyager.cloudphotos.data.fcm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DeviceTokenTest {

    @Test
    fun `of returns DeviceToken with trimmed value when input is non blank`() {
        // Arrange
        val raw = "  fcm-token-value  "

        // Act
        val token = DeviceToken.of(raw)

        // Assert
        assertEquals("fcm-token-value", token.value)
    }

    @Test
    fun `of throws when blank after trim`() {
        // Arrange
        val raw = "\n\t "

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            DeviceToken.of(raw)
        }
    }

    @Test
    fun `of throws when empty`() {
        // Arrange
        val raw = ""

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            DeviceToken.of(raw)
        }
    }
}
