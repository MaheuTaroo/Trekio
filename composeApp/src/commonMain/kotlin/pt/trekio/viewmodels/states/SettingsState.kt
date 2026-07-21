package pt.trekio.viewmodels.states

sealed interface SettingsState {
    data object Idle : SettingsState

    data object Loading : SettingsState

    data object Updated : SettingsState

    data object LoggedOut : SettingsState

    data object Deleted : SettingsState

    data class UpdateError(
        val message: String,
    ) : SettingsState

    data class LogoutError(
        val message: String,
    ) : SettingsState

    data class DeleteError(
        val message: String,
    ) : SettingsState
}
