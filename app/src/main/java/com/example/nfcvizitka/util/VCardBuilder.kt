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
            lines += "ORG:${escape(it.trim())}"
        }
        profile.jobTitle.takeIf { it.isNotBlank() }?.let {
            lines += "TITLE:${escape(it.trim())}"
        }
        profile.phone.takeIf { it.isNotBlank() }?.let {
            lines += "TEL;TYPE=CELL:${escape(ContactFormat.normalizePhoneForVCard(it))}"
        }
        profile.email.takeIf { it.isNotBlank() }?.let {
            lines += "EMAIL;TYPE=INTERNET:${escape(it.trim())}"
        }
        profile.website.takeIf { it.isNotBlank() }?.let {
            lines += "URL:${escape(ContactFormat.normalizeWebsite(it))}"
        }
        profile.note.takeIf { it.isNotBlank() }?.let {
            lines += "NOTE:${escape(it.trim())}"
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
