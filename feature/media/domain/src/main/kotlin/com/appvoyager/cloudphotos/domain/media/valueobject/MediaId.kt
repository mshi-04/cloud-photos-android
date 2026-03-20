package com.appvoyager.cloudphotos.domain.media.valueobject

@JvmInline
value class MediaId private constructor(val value: String) {

    companion object {
        fun of(raw: String): MediaId =
            raw.trim().also {
                require(it.isNotBlank()) { "MediaId must not be blank." }
            }.let(::MediaId)
    }

}
