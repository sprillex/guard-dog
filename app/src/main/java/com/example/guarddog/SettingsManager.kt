package com.example.guarddog

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "guard_dog_encrypted_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

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
