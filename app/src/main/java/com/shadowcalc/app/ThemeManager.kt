package com.shadowcalc.app

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.shadowcalc.app.R

class ThemeManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCENT = "accent_color"
        private const val DEFAULT_ACCENT = "green"
        val ACCENT_COLORS = mapOf(
            "green" to R.color.accent,
            "blue" to R.color.accent_blue,
            "red" to R.color.accent_red,
            "purple" to R.color.accent_purple,
            "orange" to R.color.accent_orange,
            "pink" to R.color.accent_pink,
            "cyan" to R.color.accent_cyan,
            "yellow" to R.color.accent_yellow,
            "white" to R.color.accent_white,
            "teal" to R.color.accent_teal
        )

        fun getAccentNameList(): List<Pair<String, String>> = listOf(
            "green" to "Neon Green",
            "blue" to "Electric Blue",
            "red" to "Crimson Red",
            "purple" to "Royal Purple",
            "orange" to "Sunset Orange",
            "pink" to "Hot Pink",
            "cyan" to "Cyber Cyan",
            "yellow" to "Neon Yellow",
            "white" to "Pure White",
            "teal" to "Deep Teal"
        )
    }

    fun applyTheme(activity: AppCompatActivity) {
        val accent = getCurrentAccent()
        val themeRes = when (accent) {
            "green" -> R.style.Theme_ShadowCalc_Green
            "blue" -> R.style.Theme_ShadowCalc_Blue
            "red" -> R.style.Theme_ShadowCalc_Red
            "purple" -> R.style.Theme_ShadowCalc_Purple
            "orange" -> R.style.Theme_ShadowCalc_Orange
            "pink" -> R.style.Theme_ShadowCalc_Pink
            "cyan" -> R.style.Theme_ShadowCalc_Cyan
            "yellow" -> R.style.Theme_ShadowCalc_Yellow
            "white" -> R.style.Theme_ShadowCalc_White
            "teal" -> R.style.Theme_ShadowCalc_Teal
            else -> R.style.Theme_ShadowCalc_Green
        }
        activity.setTheme(themeRes)
    }

    fun getCurrentAccent(): String {
        return prefs.getString(KEY_ACCENT, DEFAULT_ACCENT) ?: DEFAULT_ACCENT
    }

    fun setAccent(accent: String) {
        prefs.edit().putString(KEY_ACCENT, accent).apply()
    }

    fun getAccentColor(): Int {
        return ACCENT_COLORS[getCurrentAccent()] ?: R.color.accent
    }
}
