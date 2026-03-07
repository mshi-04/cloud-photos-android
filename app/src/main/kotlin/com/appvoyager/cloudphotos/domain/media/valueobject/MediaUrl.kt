package com.appvoyager.cloudphotos.domain.media.valueobject

import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException

@JvmInline
value class MediaUrl private constructor(val value: String) {

    companion object {
        fun of(raw: String): MediaUrl =
            raw.trim().also {
                require(it.isNotBlank()) { "MediaUrl must not be blank." }
                require(isValidUrl(it)) { "MediaUrl must be a valid URL/URI." }
            }.let(::MediaUrl)

        fun ofOrNull(raw: String?): MediaUrl? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim()
            return if (isValidUrl(trimmed)) MediaUrl(trimmed) else null
        }

        @Suppress("SwallowedException")
        private fun isValidUrl(url: String): Boolean {
            return try {
                URI(url).toURL()
                true
            } catch (_: URISyntaxException) {
                false
            } catch (_: MalformedURLException) {
                false
            }
        }
    }

}
