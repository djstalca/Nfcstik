package com.example.nfcvizitka.util

object ContactFormat {
    fun normalizeWebsite(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ""
        return if (
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun normalizePhoneForVCard(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ""

        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return trimmed

        return when {
            trimmed.startsWith("+") -> "+$digits"
            trimmed.startsWith("00") && digits.length > 2 -> "+${digits.drop(2)}"
            else -> digits
        }
    }
}
