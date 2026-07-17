package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.trekio.misc.Either
import pt.trekio.misc.Language
import pt.trekio.misc.Metric
import pt.trekio.repos.SettingsRepo
import pt.trekio.services.user.UserService
import pt.trekio.ui.theme.ThemeMode
import pt.trekio.viewmodels.states.SettingsState

class SettingsViewModel(
    private val repo: SettingsRepo,
    private val userService: UserService,
) : ViewModel() {
    companion object {
        fun getFactory(
            repo: SettingsRepo,
            service: UserService,
        ) = viewModelFactory {
            initializer {
                SettingsViewModel(repo, service)
            }
        }
    }

    private val _theme = MutableStateFlow(repo.getTheme())
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _language = MutableStateFlow(repo.getLanguage())
    val language: StateFlow<Language> = _language.asStateFlow()

    private val _metric = MutableStateFlow(repo.getMetric())
    val metric: StateFlow<Metric> = _metric.asStateFlow()

    private val _state by lazy {
        MutableStateFlow<SettingsState>(SettingsState.Idle)
    }
    val state = _state.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        repo.setTheme(mode)
        _theme.value = mode
    }

    fun setLanguage(language: Language) {
        repo.setLanguage(language)
        _language.value = language
    }

    fun setMetric(metric: Metric) {
        repo.setMetric(metric)
        _metric.value = metric
    }

    private fun updateStateAfter(
        debugAction: String,
        action: suspend () -> Either<String, *>,
        successState: (Any?) -> SettingsState,
    ) {
        _state.value = SettingsState.Loading
        viewModelScope.launch {
            val res = action()
            _state.value =
                if (res is Either.Failure) {
                    Logger.e(tag = "SettingsViewModel") { "Settings $debugAction: ${res.message}" }
                    SettingsState.Error(res.message)
                } else {
                    Logger.i(tag = "SettingsViewModel") { "Settings $debugAction succeeded" }
                    successState((res as Either.Success).value)
                }
        }
    }

    fun resetState() {
        _state.value = SettingsState.Idle
    }

    fun updateUser(
        username: String?,
        password: String?,
    ) {
        updateStateAfter("User Logout", { userService.updateDetails(username, password) }) { _ ->
            SettingsState.Updated
        }
    }

    fun logoutUser() {
        updateStateAfter("User Logout", userService::logout) { _ ->
            SettingsState.LoggedOut
        }
    }

    fun deleteUser() {
        updateStateAfter("User Deletion", userService::deleteUser) { _ ->
            SettingsState.Deleted
        }
    }
}
