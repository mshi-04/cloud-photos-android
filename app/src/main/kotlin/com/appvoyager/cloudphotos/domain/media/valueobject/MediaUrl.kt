package com.appvoyager.cloudphotos.domain.media.valueobject

import java.net.URI

@JvmInline
value class MediaUrl private constructor(val value: String) {

    companion object {
        fun of(raw: String): MediaUrl =
            raw.trim().also {
                require(it.isNotBlank()) { "MediaUrl must not be blank." }
                require(isValidUrl(it)) { "MediaUrl must be a valid URL/URI." }
            }.let(::MediaUrl)

        private fun isValidUrl(url: String): Boolean {
            return try {
                URI(url).toURL()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

}
