package pt.trekio.viewmodels.states

sealed interface UserProfileState {
    data object Idle : UserProfileState

    data object Loading : UserProfileState

    data object FetchingHikes : UserProfileState

    data object Success : UserProfileState

    data class Error(
        val message: String,
    ) : UserProfileState
}
