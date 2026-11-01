package com.appvoyager.cloudphotos.data.media.datasource

import java.util.TimeZone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class CapturedPhotoFileNameTest {

    @ParameterizedTest
    @CsvSource(
        "0, UTC, 1970-01-01-00-00-00-000",
        "1700000000123, UTC, 2023-11-14-22-13-20-123",
        "1700000000123, Asia/Tokyo, 2023-11-15-07-13-20-123",
        "0, America/Los_Angeles, 1969-12-31-16-00-00-000"
    )
    fun `create returns formatted timestamp when time and zone are fixed`(
        epochMillis: Long,
        timeZoneId: String,
        expected: String
    ) {
        // Arrange
        val timeZone = TimeZone.getTimeZone(timeZoneId)

        // Act
        // Boundary: the supplied zone controls date rollover while milliseconds retain three digits
        val actual = CapturedPhotoFileName.create(epochMillis, timeZone)

        // Assert
        assertEquals(expected, actual)
    }
}
