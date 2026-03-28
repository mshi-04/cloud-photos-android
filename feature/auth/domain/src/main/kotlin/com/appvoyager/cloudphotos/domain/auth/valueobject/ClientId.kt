package com.appvoyager.cloudphotos.domain.auth.valueobject

@JvmInline
value class ClientId private constructor(val value: String) {
    companion object {
        fun of(raw: String): ClientId = raw.trim().also {
            require(it.isNotBlank()) { "ClientId must not be blank." }
        }.let(::ClientId)
    }
}
