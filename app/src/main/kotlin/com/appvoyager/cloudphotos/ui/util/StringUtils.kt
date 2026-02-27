package com.appvoyager.cloudphotos.ui.util

object StringUtils {

    internal fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val local = parts[0]
        val domain = parts[1]
        if (local.isEmpty() || domain.isEmpty()) return email
        val maskedLocal = if (local.length <= 2) {
            "${local.first()}***"
        } else {
            local.take(2) + "*".repeat((local.length - 2).coerceAtMost(5))
        }
        return "$maskedLocal@$domain"
    }

}