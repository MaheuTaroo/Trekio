package pt.trekio.viewmodels.states

sealed interface TitleState {
    data object Loading : TitleState

    data object Failed : TitleState

    data class Success(
        val username: String,
    ) : TitleState
}
