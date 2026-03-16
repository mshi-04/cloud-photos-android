package com.appvoyager.cloudphotos.domain.media.valueobject

@JvmInline
value class MediaCreatedAt private constructor(val value: Long) {

    companion object {
        fun of(epochMillis: Long): MediaCreatedAt =
            epochMillis.also {
                require(it >= 0) { "createdAt must not be negative" }
            }.let(::MediaCreatedAt)
    }

}
