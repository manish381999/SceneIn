package com.scenein.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.scenein.utils.SP.PREF_KEY_THEME

// A Singleton object to manage theme settings application-wide
object ThemeManager {

    // These values will be stored in SharedPreferences
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_SYSTEM = "system"


    /**
     * This is the main function to apply the theme.
     * It should be called once when the application starts.
     * It reads the user's saved preference and applies it.
     */
    fun applyTheme(context: Context) {
        val selectedTheme = SP.getString(context, PREF_KEY_THEME, THEME_SYSTEM)
        applyTheme(selectedTheme)
    }

    /**
     * Applies a specific theme mode.
     * @param theme The theme to apply (e.g., THEME_LIGHT, THEME_DARK, THEME_SYSTEM).
     */
    fun applyTheme(theme: String?) {
        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    /**
     * Saves the user's new theme preference to SharedPreferences.
     */
    fun saveThemePreference(context: Context, theme: String) {
        SP.saveString(context, PREF_KEY_THEME, theme)
    }

    /**
     * Gets the currently saved theme preference.
     */
    fun getCurrentTheme(context: Context): String {
        return SP.getString(context, PREF_KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    }
}