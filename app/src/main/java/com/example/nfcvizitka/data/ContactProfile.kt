package com.example.nfcvizitka.data

data class ContactProfile(
    val firstName: String = "",
    val lastName: String = "",
    val company: String = "",
    val jobTitle: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val note: String = ""
) {
    val displayName: String
        get() = listOf(firstName.trim(), lastName.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
}
