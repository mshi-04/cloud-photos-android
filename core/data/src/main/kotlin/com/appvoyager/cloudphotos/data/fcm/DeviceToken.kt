package com.appvoyager.cloudphotos.data.fcm

@JvmInline
value class DeviceToken private constructor(val value: String) {
    companion object {
        fun of(raw: String): DeviceToken = raw.trim().also {
            require(it.isNotBlank()) { "DeviceToken must not be blank." }
        }.let(::DeviceToken)
    }
}
