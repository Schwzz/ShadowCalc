package com.shadowcalc.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurityManager(context: Context) {
    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context, "shadow_secure_prefs", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    companion object {
        private const val KEY_PIN = "secret_pin"
        private const val KEY_RECOVERY_Q = "recovery_q"
        private const val KEY_RECOVERY_A = "recovery_a"
        private const val DEFAULT_PIN = "1234"
    }
    fun getPin(): String = prefs.getString(KEY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    fun setPin(pin: String) = prefs.edit().putString(KEY_PIN, pin).apply()
    fun validatePin(input: String): Boolean = input == getPin()
    fun hasRecovery(): Boolean = prefs.contains(KEY_RECOVERY_Q)
    fun setRecovery(question: String, answer: String) {
        prefs.edit().putString(KEY_RECOVERY_Q, question).putString(KEY_RECOVERY_A, answer).apply()
    }
    fun getRecoveryQuestion(): String = prefs.getString(KEY_RECOVERY_Q, "") ?: ""
    fun validateRecoveryAnswer(answer: String): Boolean = answer == (prefs.getString(KEY_RECOVERY_A, "") ?: "")
    fun resetToDefault() { prefs.edit().clear().putString(KEY_PIN, DEFAULT_PIN).apply() }
}
