package com.example.nfcvizitka.util

import com.example.nfcvizitka.data.ContactProfile

object VCardBuilder {
    fun build(profile: ContactProfile): String {
        val lines = mutableListOf(
            "BEGIN:VCARD",
            "VERSION:3.0",
            "N:${escape(profile.lastName)};${escape(profile.firstName)};;;",
            "FN:${escape(profile.displayName)}"
        )

        profile.company.takeIf { it.isNotBlank() }?.let {
            lines += "ORG:${escape(it)}"
        }
        profile.jobTitle.takeIf { it.isNotBlank() }?.let {
            lines += "TITLE:${escape(it)}"
        }
        profile.phone.takeIf { it.isNotBlank() }?.let {
            lines += "TEL;TYPE=CELL:${escape(it)}"
        }
        profile.email.takeIf { it.isNotBlank() }?.let {
            lines += "EMAIL;TYPE=INTERNET:${escape(it)}"
        }
        profile.website.takeIf { it.isNotBlank() }?.let {
            lines += "URL:${escape(it)}"
        }
        profile.note.takeIf { it.isNotBlank() }?.let {
            lines += "NOTE:${escape(it)}"
        }

        lines += "END:VCARD"
        return lines.joinToString("\r\n", postfix = "\r\n")
    }

    internal fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\n")
        .replace(";", "\\;")
        .replace(",", "\\,")
}
