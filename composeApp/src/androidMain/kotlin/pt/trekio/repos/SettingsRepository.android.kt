package pt.trekio.repos

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import pt.trekio.misc.Language
import pt.trekio.misc.Metric
import pt.trekio.platform.TrekioAndroidApp
import pt.trekio.platform.customAppLocale
import pt.trekio.ui.theme.ThemeMode

actual class SettingsRepository actual constructor() : SettingsRepo {
    private val prefs = "trekio-prefs"
    private val themePref = "theme"
    private val languagePref = "language"

    private val metricPref = "metric"

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

    actual override fun getLanguage(): Language {
        val stored = preferences.getString(languagePref, Language.English.tag) ?: Language.English.tag
        return Language.fromTag(stored)
    }

    actual override fun setLanguage(language: Language) {
        preferences.edit { putString(languagePref, language.tag) }

        customAppLocale = language.tag
    }

    actual override fun getMetric(): Metric {
        val stored = preferences.getString(metricPref, Metric.Kilometers.name) ?: Metric.Kilometers.name
        return try {
            Metric.valueOf(stored)
        } catch (_: Exception) {
            Metric.Kilometers
        }
    }

    actual override fun setMetric(metric: Metric) {
        preferences.edit { putString(metricPref, metric.name) }
    }
}
