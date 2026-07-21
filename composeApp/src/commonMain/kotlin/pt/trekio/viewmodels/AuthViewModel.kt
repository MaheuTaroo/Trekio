package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.trekio.misc.Either
import pt.trekio.misc.Email
import pt.trekio.misc.Password
import pt.trekio.misc.Username
import pt.trekio.services.user.UserService
import pt.trekio.viewmodels.states.AuthState
import kotlin.getValue

class AuthViewModel(
    private val userService: UserService,
) : ViewModel() {
    companion object {
        fun getFactory(service: UserService) =
            viewModelFactory {
                initializer {
                    AuthViewModel(service)
                }
            }
    }

    private val _state by lazy {
        MutableStateFlow<AuthState>(AuthState.Idle)
    }
    val state = _state.asStateFlow()

    private val _googleState by lazy {
        MutableStateFlow<String?>(null)
    }
    val googleState = _googleState.asStateFlow()

    fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
    ) {
        val usernameResult = runCatching { Username(username) }
        val emailResult = runCatching { Email(email) }
        val passwordResult = runCatching { Password(password) }

        when {
            usernameResult.isFailure ->
                _state.value = AuthState.Error(usernameResult.exceptionOrNull()?.message ?: "Invalid username")
            emailResult.isFailure ->
                _state.value = AuthState.Error(emailResult.exceptionOrNull()?.message ?: "Invalid email")
            passwordResult.isFailure ->
                _state.value = AuthState.Error(passwordResult.exceptionOrNull()?.message ?: "Invalid password")
            password != confirmPassword ->
                _state.value = AuthState.Error("Passwords do not match")
            else -> {
                _state.value = AuthState.Loading
                viewModelScope.launch {
                    val res = userService.signUp(username, email, password)
                    _state.value =
                        if (res is Either.Failure) {
                            Logger.e(tag = "AuthViewModel") { "Register failed: ${res.message}" }
                            AuthState.Error(res.message)
                        } else {
                            Logger.i(tag = "AuthViewModel") { "Register succeeded" }
                            AuthState.Success
                        }
                }
            }
        }
    }

    fun login(
        email: String,
        password: String,
    ) {
        val emailResult = runCatching { Email(email) }
        val passwordResult = runCatching { Password(password) }

        when {
            emailResult.isFailure ->
                _state.value = AuthState.Error(emailResult.exceptionOrNull()?.message ?: "Invalid email")
            passwordResult.isFailure ->
                _state.value = AuthState.Error(passwordResult.exceptionOrNull()?.message ?: "Invalid password")
            else -> {
                _state.value = AuthState.Loading
                viewModelScope.launch {
                    val res = userService.login(email, password)
                    _state.value =
                        if (res is Either.Failure) {
                            Logger.e(tag = "AuthViewModel") { "Login failed: ${res.message}" }
                            AuthState.Error(res.message)
                        } else {
                            Logger.i(tag = "AuthViewModel") { "Login succeeded" }
                            AuthState.Success
                        }
                }
            }
        }
    }

    fun googleAuth() {
        viewModelScope.launch {
            val res = userService.googlePopup()
            if (res is Either.Success) _googleState.value = res.value
        }
    }

    fun cleanupGoogle() {
        _googleState.value = null
    }

    fun updateUser(
        username: String?,
        password: String?,
    ) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            val res = userService.updateDetails(username, password)
            _state.value =
                if (res is Either.Failure) {
                    Logger.e(tag = "AuthViewModel") { "Update failed: ${res.message}" }
                    AuthState.OAuthError(res.message)
                } else {
                    Logger.i(tag = "AuthViewModel") { "Update succeeded" }
                    AuthState.Success
                }
        }
    }
}
