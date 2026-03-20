package com.appvoyager.cloudphotos.ui.util

object ValidationUtils {
    private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun isValidEmailFormat(email: String): Boolean = EMAIL_REGEX.matches(email.trim())
}