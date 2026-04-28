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
import pt.trekio.services.user.UserService

sealed interface UserProfileState {
    data object Idle : UserProfileState

    data object Loading : UserProfileState

    data object Success : UserProfileState

    data class Error(
        val message: String,
    ) : UserProfileState
}

class UserProfileViewModel(
    private val userService: UserService,
) : ViewModel() {
    companion object {
        fun getFactory(service: UserService) =
            viewModelFactory {
                initializer {
                    UserProfileViewModel(service)
                }
            }
    }

    private val _state by lazy {
        MutableStateFlow<UserProfileState>(UserProfileState.Idle)
    }
    val state = _state.asStateFlow()

    fun delete() {
        _state.value = UserProfileState.Loading
        viewModelScope.launch {
            val res = userService.delete()
            _state.value =
                if (res is Either.Failure) {
                    Logger.e("UserProfileViewModel") { "User deletion failure: ${res.message}" }
                    UserProfileState.Error(res.message)
                } else {
                    Logger.i("UserProfileViewModel") { "User deletion succeeded" }
                    UserProfileState.Success
                }
        }
    }
}
