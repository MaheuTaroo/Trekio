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
import pt.trekio.misc.Success
import pt.trekio.repos.UserRepository
import pt.trekio.services.user.UserService
import pt.trekio.viewmodels.states.UserProfileState

class UserProfileViewModel(
    private val userService: UserService,
    private val userRepo: UserRepository,
) : ViewModel() {
    companion object {
        fun getFactory(
            service: UserService,
            repo: UserRepository,
        ) = viewModelFactory {
            initializer {
                UserProfileViewModel(
                    service,
                    repo,
                )
            }
        }
    }

    private val _state by lazy {
        MutableStateFlow<UserProfileState>(UserProfileState.Idle)
    }
    val state = _state.asStateFlow()

    fun statistics() {
        _state.value = UserProfileState.Loading
        viewModelScope.launch {
            val user = userRepo.getOwnDetails()
            if (user == null) {
                val message = "Could not find own statistics"
                Logger.e(tag = "UserProfileViewModel") { "User Profile Details failure: $message" }
                UserProfileState.Error(message)
                return@launch
            }
            val res = userService.getStatsOf(user.id)
            _state.value =
                if (res is Either.Failure) {
                    Logger.e(tag = "UserProfileViewModel") { "User Profile Details failure: ${res.message}" }
                    UserProfileState.Error(res.message)
                } else {
                    Logger.i(tag = "UserProfileViewModel") { "User Profile Details succeeded" }
                    UserProfileState.Success((res as Success).value)
                }
        }
    }
}
