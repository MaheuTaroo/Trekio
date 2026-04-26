package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.trekio.misc.Either
import pt.trekio.services.UserService

sealed interface LoginState {
    data object Idle : LoginState

    data object Loading : LoginState

    data object Success : LoginState

    data class Error(
        val message: String,
    ) : LoginState
}

class LoginViewModel(
    private val userService: UserService,
) : ViewModel() {
    companion object {
        fun getFactory(service: UserService) =
            viewModelFactory {
                initializer {
                    LoginViewModel(service)
                }
            }
    }

    private val _state by lazy {
        MutableStateFlow<LoginState>(LoginState.Idle)
    }
    val state = _state.asStateFlow()

    fun login(
        email: String,
        password: String,
    ) {
        val verifyEmail = email.isValidEmail()
        val verifyPassword = password.isSafePassword()
        when {
            !verifyEmail.first ->
                _state.value = LoginState.Error(verifyEmail.second)
            !verifyPassword.first ->
                _state.value = LoginState.Error(verifyPassword.second)
            else -> {
                _state.value = LoginState.Loading
                viewModelScope.launch {
                    val res = userService.login(email, password)
                    _state.value =
                        if (res is Either.Failure) {
                            LoginState.Error(res.message)
                        } else {
                            LoginState.Success
                        }
                }
            }
        }
    }
}
