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

sealed interface SignUpState {
    data object Idle : SignUpState

    data object Loading : SignUpState

    data object Success : SignUpState

    data class Error(
        val message: String,
    ) : SignUpState
}

class SignUpViewModel(
    private val userService: UserService,
) : ViewModel() {
    companion object {
        fun getFactory(service: UserService) =
            viewModelFactory {
                initializer {
                    SignUpViewModel(service)
                }
            }
    }

    private val _state by lazy {
        MutableStateFlow<SignUpState>(SignUpState.Idle)
    }
    val state = _state.asStateFlow()

    fun signUp(
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
    ) {
        val verifyName = username.isValidName()
        val verifyEmail = email.isValidEmail()
        val verifyPassword = password.isSafePassword()
        when {
            !verifyName.first ->
                _state.value = SignUpState.Error(verifyName.second)
            !verifyEmail.first ->
                _state.value = SignUpState.Error(verifyEmail.second)
            !verifyPassword.first ->
                _state.value = SignUpState.Error(verifyPassword.second)
            password != confirmPassword ->
                _state.value = SignUpState.Error("Passwords do not match")
            else -> {
                _state.value = SignUpState.Loading
                viewModelScope.launch {
                    val res = userService.signUp(username, email, password)
                    _state.value =
                        if (res is Either.Failure) {
                            SignUpState.Error(res.message)
                        } else {
                            SignUpState.Success
                        }
                }
            }
        }
    }
}
