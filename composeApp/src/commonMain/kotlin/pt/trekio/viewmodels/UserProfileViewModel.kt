package pt.trekio.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.trekio.dto.UserDto
import pt.trekio.misc.Either
import pt.trekio.misc.Success
import pt.trekio.services.user.UserService
import pt.trekio.viewmodels.states.UserProfileState

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

    private fun updateStateAfter(
        debugAction: String,
        operation: suspend () -> Either<String, *>,
        expectedStateFactory: (Any?) -> UserProfileState,
    ) {
        _state.value = UserProfileState.Loading
        viewModelScope.launch {
            val res = operation()
            _state.value =
                if (res is Either.Failure) {
                    Logger.e(tag = "UserProfileViewModel") { "User $debugAction failure: ${res.message}" }
                    UserProfileState.Error(res.message)
                } else {
                    Logger.i(tag = "UserProfileViewModel") { "User $debugAction succeeded" }
                    expectedStateFactory((res as Success).value)
                }
        }
    }

    private val _state by lazy {
        MutableStateFlow<UserProfileState>(UserProfileState.Idle)
    }
    val state = _state.asStateFlow()

    fun profileDetails() {
        updateStateAfter("profile details", userService::getOwnDetails) {
            UserProfileState.Success(it as UserDto)
        }
    }

    fun delete() {
        updateStateAfter("deletion", userService::deleteUser) { _ ->
            UserProfileState.Deleted
        }
    }

    fun logout() {
        updateStateAfter("logout", userService::logout) { _ ->
            UserProfileState.LoggedOut
        }
    }
}
