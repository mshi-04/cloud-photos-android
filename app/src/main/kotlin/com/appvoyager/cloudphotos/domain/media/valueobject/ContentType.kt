package com.appvoyager.cloudphotos.domain.media.valueobject

@JvmInline
value class ContentType private constructor(val value: String) {

    companion object {
        private val ALLOWED_PREFIXES = listOf("image/", "video/")

        fun of(raw: String): ContentType =
            raw.trim().also {
                require(it.isNotBlank()) { "ContentType must not be blank." }
                require(ALLOWED_PREFIXES.any(it::startsWith)) {
                    "ContentType must be an image or video type."
                }
            }.let(::ContentType)
    }

}