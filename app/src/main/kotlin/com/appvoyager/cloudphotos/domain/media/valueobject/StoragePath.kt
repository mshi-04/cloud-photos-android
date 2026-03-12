package com.appvoyager.cloudphotos.domain.media.valueobject

@JvmInline
value class StoragePath private constructor(val value: String) {

    companion object {
        fun of(raw: String): StoragePath =
            raw.trim().also {
                require(it.isNotBlank()) { "StoragePath must not be blank." }
            }.let(::StoragePath)
    }

}
