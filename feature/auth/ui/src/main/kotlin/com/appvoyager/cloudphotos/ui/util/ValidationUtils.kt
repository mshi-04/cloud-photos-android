package com.appvoyager.cloudphotos.ui.util

import com.appvoyager.cloudphotos.domain.auth.valueobject.Email

object ValidationUtils {
    fun isValidEmailFormat(email: String): Boolean = runCatching { Email.of(email) }.isSuccess
}
