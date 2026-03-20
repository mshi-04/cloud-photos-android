package com.appvoyager.cloudphotos.domain.auth.valueobject

@JvmInline
value class ConfirmationCode private constructor(val value: String) {

    companion object {
        fun of(raw: String): ConfirmationCode =
            raw.trim().also {
                require(it.isNotEmpty()) { "ConfirmationCode must not be blank." }
                require(it.length == 6 && it.all(Char::isDigit)) {
                    "ConfirmationCode must be 6 digits."
                }
            }.let(::ConfirmationCode)
    }

}
