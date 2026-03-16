package com.appvoyager.cloudphotos.domain.media.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MediaTypeTest {

    @Test
    fun `fromContentType returns IMAGE for image jpeg`() {
        assertEquals(MediaType.IMAGE, MediaType.fromContentType("image/jpeg"))
    }

    @Test
    fun `fromContentType returns IMAGE for image png`() {
        assertEquals(MediaType.IMAGE, MediaType.fromContentType("image/png"))
    }

    @Test
    fun `fromContentType returns VIDEO for video mp4`() {
        assertEquals(MediaType.VIDEO, MediaType.fromContentType("video/mp4"))
    }

    @Test
    fun `fromContentType returns VIDEO for video quicktime`() {
        assertEquals(MediaType.VIDEO, MediaType.fromContentType("video/quicktime"))
    }

    @Test
    fun `fromContentType returns IMAGE for unknown content type`() {
        assertEquals(MediaType.IMAGE, MediaType.fromContentType("application/octet-stream"))
    }
}
