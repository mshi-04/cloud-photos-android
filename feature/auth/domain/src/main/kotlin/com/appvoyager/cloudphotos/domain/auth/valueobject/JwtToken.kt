package com.appvoyager.cloudphotos.domain.auth.valueobject

@JvmInline
value class JwtToken private constructor(val value: String) {

    companion object {
        fun of(raw: String): JwtToken =
            raw.trim().also {
                require(it.isNotEmpty()) { "Token must not be blank." }
            }.let(::JwtToken)
    }

}
