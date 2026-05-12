package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel

enum class Theme(val value: String) {
    SYSTEM_BASED("System Based"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class Language(val value: String) {
    ENGLISH("English"),
    PORTUGUESE("Portuguese"),
}

class SettingsViewModel: ViewModel() {
}