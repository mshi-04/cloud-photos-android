package com.appvoyager.cloudphotos.domain.media.valueobject

@JvmInline
value class MediaUrl private constructor(val value: String) {

    companion object {
        fun of(raw: String): MediaUrl =
            raw.trim().also {
                require(it.isNotBlank()) { "MediaUrl must not be blank." }
            }.let(::MediaUrl)
    }

}
