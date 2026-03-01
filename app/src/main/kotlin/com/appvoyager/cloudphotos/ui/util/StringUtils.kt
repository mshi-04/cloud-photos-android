package com.appvoyager.cloudphotos.ui.util

import com.appvoyager.cloudphotos.domain.auth.valueobject.Email

object StringUtils {

    private const val VISIBLE_LOCAL_CHARS = 2
    private const val MAX_MASK_ASTERISKS = 5

    internal fun maskEmail(email: Email): String {
        val parts = email.value.split("@")
        if (parts.size != 2) return email.value
        val local = parts[0]
        val domain = parts[1]
        if (local.isEmpty() || domain.isEmpty()) return email.value
        val maskedLocal = if (local.length <= VISIBLE_LOCAL_CHARS) {
            "${local.first()}***"
        } else {
            local.take(VISIBLE_LOCAL_CHARS) + "*".repeat(
                (local.length - VISIBLE_LOCAL_CHARS).coerceAtMost(MAX_MASK_ASTERISKS)
            )
        }
        return "$maskedLocal@$domain"
    }

}
