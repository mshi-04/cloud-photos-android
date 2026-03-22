package com.appvoyager.cloudphotos.domain.media.valueobject

@JvmInline
value class UploadSuccessCount private constructor(val value: Int) {

    companion object {
        fun of(raw: Int): UploadSuccessCount {
            require(raw > 0) { "UploadSuccessCount must be positive." }
            return UploadSuccessCount(raw)
        }
    }

}
