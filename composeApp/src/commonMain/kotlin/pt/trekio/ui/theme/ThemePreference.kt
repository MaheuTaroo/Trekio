package pt.trekio.ui.theme

interface ThemePreference {
    val current: ThemeMode

    fun set(mode: ThemeMode)
}
