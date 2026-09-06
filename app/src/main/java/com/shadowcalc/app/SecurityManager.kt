package com.shadowcalc.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class SecurityManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context, "shadow_secure_prefs_v5", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    companion object {
        private const val KEY_PIN = "secret_pin"
        private const val KEY_DECOY_PIN = "decoy_pin"
        private const val KEY_RECOVERY_Q = "recovery_q"
        private const val KEY_RECOVERY_A = "recovery_a"
        private const val KEY_THEME_ACCENT = "theme_accent"
        private const val KEY_FIRST_TIME = "first_time_done"
        private const val KEY_AUTO_LOCK_MINUTES = "auto_lock_minutes"
        private const val DEFAULT_PIN = "1234"
        private const val SALT = "ShadowCalcV5Salt!!"
    }

    fun isFirstTime(): Boolean = !prefs.getBoolean(KEY_FIRST_TIME, false)
    fun setFirstTimeDone() = prefs.edit().putBoolean(KEY_FIRST_TIME, true).apply()

    fun getPin(): String = prefs.getString(KEY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    fun setPin(pin: String) = prefs.edit().putString(KEY_PIN, pin).apply()
    fun validatePin(input: String): Boolean = input == getPin()

    fun getDecoyPin(): String? = prefs.getString(KEY_DECOY_PIN, null)
    fun setDecoyPin(pin: String) = prefs.edit().putString(KEY_DECOY_PIN, pin).apply()
    fun hasDecoyPin(): Boolean = prefs.contains(KEY_DECOY_PIN)
    fun validateDecoyPin(input: String): Boolean = input == getDecoyPin()
    fun clearDecoyPin() = prefs.edit().remove(KEY_DECOY_PIN).apply()

    fun hasRecovery(): Boolean = prefs.contains(KEY_RECOVERY_Q)
    fun setRecovery(question: String, answer: String) {
        prefs.edit().putString(KEY_RECOVERY_Q, question).putString(KEY_RECOVERY_A, answer).apply()
    }
    fun getRecoveryQuestion(): String = prefs.getString(KEY_RECOVERY_Q, "") ?: ""
    fun validateRecoveryAnswer(answer: String): Boolean = answer == (prefs.getString(KEY_RECOVERY_A, "") ?: "")

    fun getThemeAccent(): String = prefs.getString(KEY_THEME_ACCENT, "green") ?: "green"
    fun setThemeAccent(accent: String) = prefs.edit().putString(KEY_THEME_ACCENT, accent).apply()

    fun getAutoLockMinutes(): Int = prefs.getInt(KEY_AUTO_LOCK_MINUTES, 5)
    fun setAutoLockMinutes(minutes: Int) = prefs.edit().putInt(KEY_AUTO_LOCK_MINUTES, minutes).apply()

    fun resetToDefault() {
        prefs.edit().clear().putString(KEY_PIN, DEFAULT_PIN).apply()
    }

    fun deriveVaultKey(pin: String? = null): SecretKeySpec {
        val pass = (pin ?: getPin()) + SALT
        val spec = PBEKeySpec(pass.toCharArray(), SALT.toByteArray(), 100000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
