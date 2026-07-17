package pt.trekio.repos

import pt.trekio.misc.Language
import pt.trekio.misc.Metric
import pt.trekio.ui.theme.ThemeMode

actual class SettingsRepository actual constructor() : SettingsRepo {
    actual override fun getTheme(): ThemeMode {
        TODO("Not yet implemented")
    }

    actual override fun setTheme(theme: ThemeMode) {}

    actual override fun getLanguage(): Language {
        TODO("Not yet implemented")
    }

    actual override fun setLanguage(language: Language) {
        TODO("Not yet implemented")
    }

    actual override fun getMetric(): Metric {
        TODO("Not yet implemented")
    }

    actual override fun setMetric(metric: Metric) {
        TODO("Not yet implemented")
    }
}
