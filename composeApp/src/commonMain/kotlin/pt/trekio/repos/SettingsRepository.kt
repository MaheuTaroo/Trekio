package pt.trekio.repos

import pt.trekio.misc.Language
import pt.trekio.ui.theme.ThemeMode

interface SettingsRepo {
    fun getTheme(): ThemeMode

    fun setTheme(theme: ThemeMode)

    fun getLanguage(): Language

    fun setLanguage(language: Language)
}

expect class SettingsRepository() : SettingsRepo {
    override fun getTheme(): ThemeMode

    override fun setTheme(theme: ThemeMode)

    override fun getLanguage(): Language

    override fun setLanguage(language: Language)
}
