package com.appvoyager.cloudphotos.ui.util

import com.appvoyager.cloudphotos.domain.auth.valueobject.Email

object StringUtils {

    internal fun maskEmail(email: Email): String {
        val parts = email.value.split("@")
        if (parts.size != 2) return email.value
        val local = parts[0]
        val domain = parts[1]
        if (local.isEmpty() || domain.isEmpty()) return email.value
        val maskedLocal = if (local.length <= 2) {
            "${local.first()}***"
        } else {
            local.take(2) + "*".repeat((local.length - 2).coerceAtMost(5))
        }
        return "$maskedLocal@$domain"
    }

}
