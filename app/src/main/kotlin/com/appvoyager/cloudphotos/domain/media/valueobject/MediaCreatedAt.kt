package com.appvoyager.cloudphotos.domain.media.valueobject

@JvmInline
value class MediaCreatedAt private constructor(val value: Long) {

    companion object {
        fun of(epochMillis: Long): MediaCreatedAt = MediaCreatedAt(epochMillis)
    }

}
