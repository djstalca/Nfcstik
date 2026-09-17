package com.example.nfcvizitka.data

import android.content.Context

class ContactRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ContactProfile = ContactProfile(
        firstName = prefs.getString(KEY_FIRST_NAME, "") ?: "",
        lastName = prefs.getString(KEY_LAST_NAME, "") ?: "",
        company = prefs.getString(KEY_COMPANY, "") ?: "",
        jobTitle = prefs.getString(KEY_JOB_TITLE, "") ?: "",
        phone = prefs.getString(KEY_PHONE, "") ?: "",
        email = prefs.getString(KEY_EMAIL, "") ?: "",
        website = prefs.getString(KEY_WEBSITE, "") ?: "",
        note = prefs.getString(KEY_NOTE, "") ?: ""
    )

    fun save(profile: ContactProfile) {
        prefs.edit()
            .putString(KEY_FIRST_NAME, profile.firstName.trim())
            .putString(KEY_LAST_NAME, profile.lastName.trim())
            .putString(KEY_COMPANY, profile.company.trim())
            .putString(KEY_JOB_TITLE, profile.jobTitle.trim())
            .putString(KEY_PHONE, profile.phone.trim())
            .putString(KEY_EMAIL, profile.email.trim())
            .putString(KEY_WEBSITE, profile.website.trim())
            .putString(KEY_NOTE, profile.note.trim())
            .apply()
    }

    fun isNfcSharingEnabled(): Boolean = prefs.getBoolean(KEY_NFC_ENABLED, false)

    fun setNfcSharingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NFC_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "nfc_vizitka"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_COMPANY = "company"
        private const val KEY_JOB_TITLE = "job_title"
        private const val KEY_PHONE = "phone"
        private const val KEY_EMAIL = "email"
        private const val KEY_WEBSITE = "website"
        private const val KEY_NOTE = "note"
        private const val KEY_NFC_ENABLED = "nfc_enabled"
    }
}
