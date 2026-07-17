package pt.trekio.viewmodels.states

import pt.trekio.dto.UserDto

sealed interface UserProfileState {
    data object Idle : UserProfileState

    data object Loading : UserProfileState

    data class Success(
        val user: UserDto?,
    ) : UserProfileState

    data class Error(
        val message: String,
    ) : UserProfileState
}
