package com.appvoyager.cloudphotos.domain.media.valueobject

@JvmInline
value class MediaUploadedAt private constructor(val value: Long) {

    companion object {
        fun of(epochMillis: Long): MediaUploadedAt =
            epochMillis.also {
                require(it >= 0) { "uploadedAt must not be negative" }
            }.let(::MediaUploadedAt)
    }

}
