package com.appvoyager.cloudphotos.domain.media.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
}
