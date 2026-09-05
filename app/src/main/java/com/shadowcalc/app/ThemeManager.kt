package com.shadowcalc.app

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat

object ThemeManager {
    private val accentMap = mapOf(
        "green" to Pair(0xFF00E676, 0xFF00C853),
        "blue" to Pair(0xFF448AFF, 0xFF2962FF),
        "purple" to Pair(0xFFE040FB, 0xFFD500F9),
        "orange" to Pair(0xFFFFAB40, 0xFFFF6D00),
        "red" to Pair(0xFFFF5252, 0xFFD50000),
        "teal" to Pair(0xFF64FFDA, 0xFF00BFA5),
        "pink" to Pair(0xFFFF4081, 0xFFC51162),
        "yellow" to Pair(0xFFFFFF00, 0xFFFFD600),
    )

    fun applyTheme(activity: Activity, securityManager: SecurityManager) {
        val accent = securityManager.getThemeAccent()
        val (primary, dark) = accentMap[accent] ?: accentMap["green"]!!
        // Theme is applied via Material3 dynamic color override in onCreate
        // For V4 we store the choice; actual color swap requires regenerating theme or using runtime color
    }

    fun getAccentColorInt(context: Context, securityManager: SecurityManager): Int {
        val accent = securityManager.getThemeAccent()
        return when (accent) {
            "green" -> 0xFF00E676.toInt()
            "blue" -> 0xFF448AFF.toInt()
            "purple" -> 0xFFE040FB.toInt()
            "orange" -> 0xFFFFAB40.toInt()
            "red" -> 0xFFFF5252.toInt()
            "teal" -> 0xFF64FFDA.toInt()
            "pink" -> 0xFFFF4081.toInt()
            "yellow" -> 0xFFFFFF00.toInt()
            else -> 0xFF00E676.toInt()
        }
    }

    fun getAccentNameList(): List<Pair<String, String>> = listOf(
        "green" to "Neon Green",
        "blue" to "Electric Blue",
        "purple" to "Vivid Purple",
        "orange" to "Sunset Orange",
        "red" to "Crimson Red",
        "teal" to "Aqua Teal",
        "pink" to "Hot Pink",
        "yellow" to "Solar Yellow"
    )
}
