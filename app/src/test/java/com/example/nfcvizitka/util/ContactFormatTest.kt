package com.example.nfcvizitka.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactFormatTest {
    @Test
    fun normalizesInternationalPhone() {
        assertEquals("+38630342529", ContactFormat.normalizePhoneForVCard("+386 30 342 529"))
        assertEquals("+38630342529", ContactFormat.normalizePhoneForVCard("00386 30 342 529"))
    }

    @Test
    fun keepsLocalPhoneAsDigits() {
        assertEquals("030342529", ContactFormat.normalizePhoneForVCard("030 342 529"))
    }

    @Test
    fun addsHttpsToWebsiteWhenMissing() {
        assertEquals("https://prodent.si", ContactFormat.normalizeWebsite("prodent.si"))
        assertEquals("https://prodent.si", ContactFormat.normalizeWebsite("https://prodent.si"))
    }
}
