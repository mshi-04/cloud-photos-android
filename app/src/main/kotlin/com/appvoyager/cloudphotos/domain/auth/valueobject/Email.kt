package com.appvoyager.cloudphotos.domain.auth.valueobject

@JvmInline
value class Email private constructor(val value: String) {

    companion object {
        private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

        fun of(raw: String): Email =
            raw.trim().also {
                require(it.isNotEmpty()) { "Email must not be blank." }
                require(EMAIL_REGEX.matches(it)) { "Invalid email format." }
            }.let(::Email)
    }

}
