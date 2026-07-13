package pt.trekio.repos

import pt.trekio.ui.theme.ThemeMode

actual class SettingsRepository actual constructor() : SettingsRepo {
    actual override fun getTheme(): ThemeMode {
        TODO("Not yet implemented")
    }

    actual override fun setTheme(theme: ThemeMode) {
    }

    actual override fun getLanguage(): String {
        TODO("Not yet implemented")
    }

    actual override fun setLanguage(language: String) {
    }
}
