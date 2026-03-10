package com.appvoyager.cloudphotos.domain.auth.valueobject

@JvmInline
value class UserId private constructor(val value: String) {

    companion object {
        fun of(raw: String): UserId =
            raw.trim().also {
                require(it.isNotBlank()) { "UserId must not be blank." }
            }.let(::UserId)
    }

}