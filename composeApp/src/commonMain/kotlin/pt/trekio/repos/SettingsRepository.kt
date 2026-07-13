package pt.trekio.repos

import pt.trekio.ui.theme.ThemeMode

interface SettingsRepo {
    fun getTheme(): ThemeMode

    fun setTheme(theme: ThemeMode)

    fun getLanguage(): String

    fun setLanguage(language: String)
}

expect class SettingsRepository() : SettingsRepo {
    override fun getTheme(): ThemeMode

    override fun setTheme(theme: ThemeMode)

    override fun getLanguage(): String

    override fun setLanguage(language: String)
}
