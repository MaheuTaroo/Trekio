package pt.trekio.repos

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import pt.trekio.platform.TrekioAndroidApp
import pt.trekio.ui.theme.ThemeMode

actual class SettingsRepository actual constructor() : SettingsRepo {
    private val prefs = "trekio-prefs"
    private val themePref = "theme"
    private val languagePref = "language"

    private val preferences: SharedPreferences = TrekioAndroidApp.appContext.getSharedPreferences(prefs, Context.MODE_PRIVATE)

    actual override fun getTheme(): ThemeMode {
        val stored = preferences.getString(themePref, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(stored)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    actual override fun setTheme(theme: ThemeMode) {
        preferences.edit { putString(themePref, theme.name) }
    }

    actual override fun getLanguage(): String = preferences.getString(languagePref, "en") ?: "en"

    actual override fun setLanguage(language: String) {
        preferences.edit { putString(languagePref, language) }
    }
}
