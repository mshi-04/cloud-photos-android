package com.appvoyager.cloudphotos.domain.media.valueobject

@JvmInline
value class CloudStoragePath private constructor(val value: String) {

    companion object {
        fun of(raw: String): CloudStoragePath =
            raw.trim().also {
                require(it.isNotBlank()) { "CloudStoragePath must not be blank." }
            }.let(::CloudStoragePath)
    }

}
