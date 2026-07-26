package pt.trekio.services

import pt.trekio.misc.Language
import pt.trekio.misc.Metric
import pt.trekio.repos.SettingsRepo
import pt.trekio.services.utils.TestSettings.language
import pt.trekio.services.utils.TestSettings.metric
import pt.trekio.services.utils.TestSettings.theme
import pt.trekio.ui.theme.ThemeMode

object SuccessfulSettingsRepository : SettingsRepo {
    override fun getTheme(): ThemeMode = theme

    override fun setTheme(theme: ThemeMode) {}

    override fun getLanguage(): Language = language

    override fun setLanguage(language: Language) {}

    override fun getMetric(): Metric = metric

    override fun setMetric(metric: Metric) {}
}
