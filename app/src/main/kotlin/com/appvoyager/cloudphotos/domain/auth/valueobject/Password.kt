package com.appvoyager.cloudphotos.domain.auth.valueobject

@JvmInline
value class Password private constructor(val value: String) {

    companion object {
        fun of(raw: String): Password =
            raw.trim().also {
                require(it.length >= 8) { "Password must be at least 8 characters." }
            }.let(::Password)
    }

}
