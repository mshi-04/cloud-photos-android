package com.appvoyager.cloudphotos.domain.auth.valueobject

@JvmInline
value class ConfirmationCode private constructor(val value: String) {
    companion object {
        fun of(raw: String): ConfirmationCode = raw.trim().also {
            require(it.isNotEmpty()) { "ConfirmationCode must not be blank." }
            require(it.codePointCount(0, it.length) == 6) { "ConfirmationCode must be 6 characters." }
            require(it.all(Char::isDigit)) { "ConfirmationCode must contain only digits." }
        }.toAsciiDigits().let(::ConfirmationCode)

        private fun String.toAsciiDigits(): String = map(Char::digitToInt).joinToString("")
    }
}
