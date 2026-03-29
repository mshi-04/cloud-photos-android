package com.appvoyager.cloudphotos.domain.media.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MediaTypeTest {

    @Test
    fun `fromContentType returns image when content type is image jpeg`() {
        assertEquals(MediaType.IMAGE, MediaType.fromContentType("image/jpeg"))
    }

    @Test
    fun `fromContentType returns image when content type is image png`() {
        assertEquals(MediaType.IMAGE, MediaType.fromContentType("image/png"))
    }

    @Test
    fun `fromContentType returns video when content type is video mp4`() {
        assertEquals(MediaType.VIDEO, MediaType.fromContentType("video/mp4"))
    }

    @Test
    fun `fromContentType returns video when content type is video quicktime`() {
        assertEquals(MediaType.VIDEO, MediaType.fromContentType("video/quicktime"))
    }

    @Test
    fun `fromContentType returns image when content type is unknown`() {
        assertEquals(MediaType.IMAGE, MediaType.fromContentType("application/octet-stream"))
    }
}
