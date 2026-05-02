package com.example.guarddog

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("guard_dog_prefs", Context.MODE_PRIVATE)

    var trustedContactName: String?
        get() = prefs.getString(KEY_CONTACT_NAME, null)
        set(value) = prefs.edit().putString(KEY_CONTACT_NAME, value).apply()

    var trustedContactPhone: String?
        get() = prefs.getString(KEY_CONTACT_PHONE, null)
        set(value) = prefs.edit().putString(KEY_CONTACT_PHONE, value).apply()

    companion object {
        private const val KEY_CONTACT_NAME = "trusted_contact_name"
        private const val KEY_CONTACT_PHONE = "trusted_contact_phone"
    }
}
