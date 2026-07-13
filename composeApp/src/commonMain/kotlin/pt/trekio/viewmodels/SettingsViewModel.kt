package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pt.trekio.repos.SettingsRepo
import pt.trekio.ui.theme.ThemeMode

class SettingsViewModel(
    private val repo: SettingsRepo,
) : ViewModel() {
    private val _theme = MutableStateFlow(repo.getTheme())
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _language = MutableStateFlow(repo.getLanguage())
    val language: StateFlow<String> = _language.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        repo.setTheme(mode)
        _theme.value = mode
    }

    fun setLanguage(language: String) {
        repo.setLanguage(language)
        _language.value = language
    }
}
