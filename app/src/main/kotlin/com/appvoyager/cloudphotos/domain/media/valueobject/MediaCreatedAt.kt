package com.appvoyager.cloudphotos.domain.media.valueobject

/**
 * Represents the creation time of a media item.
 * [value] is the epoch time in milliseconds.
 */
@JvmInline
value class MediaCreatedAt private constructor(val value: Long) {
    companion object {
        fun of(epochMillis: Long): MediaCreatedAt = MediaCreatedAt(epochMillis)
    }
}
