package com.example.nfcvizitka.util

import com.example.nfcvizitka.data.ContactProfile
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardBuilderTest {
    @Test
    fun buildsExpectedContactFields() {
        val card = VCardBuilder.build(
            ContactProfile(
                firstName = "Ana",
                lastName = "Novak",
                company = "Primer d.o.o.",
                jobTitle = "Specialist",
                phone = "+38640123456",
                email = "ana@example.com"
            )
        )

        assertTrue(card.startsWith("BEGIN:VCARD\r\nVERSION:3.0\r\n"))
        assertTrue(card.contains("FN:Ana Novak"))
        assertTrue(card.contains("ORG:Primer d.o.o."))
        assertTrue(card.contains("TEL;TYPE=CELL:+38640123456"))
        assertTrue(card.endsWith("END:VCARD\r\n"))
    }

    @Test
    fun escapesReservedCharacters() {
        val escaped = VCardBuilder.escape("a,b;c\\d\ne")
        assertTrue(escaped == "a\\,b\\;c\\\\d\\ne")
    }
}
