package pt.trekio.repos

import pt.trekio.misc.Language
import pt.trekio.misc.Metric
import pt.trekio.ui.theme.ThemeMode

interface SettingsRepo {
    fun getTheme(): ThemeMode

    fun setTheme(theme: ThemeMode)

    fun getLanguage(): Language

    fun setLanguage(language: Language)

    fun getMetric(): Metric

    fun setMetric(metric: Metric)
}

expect class SettingsRepository() : SettingsRepo {
    override fun getTheme(): ThemeMode

    override fun setTheme(theme: ThemeMode)

    override fun getLanguage(): Language

    override fun setLanguage(language: Language)

    override fun getMetric(): Metric

    override fun setMetric(metric: Metric)
}
