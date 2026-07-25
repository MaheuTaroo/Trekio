package pt.trekio.viewmodels.states

import pt.trekio.dto.TrailDto

sealed interface TrailFetchState {
    data object Idle : TrailFetchState

    data object Loading : TrailFetchState

    data class TrailsSuccess(
        val trails: List<TrailDto>,
    ) : TrailFetchState

    data object Success : TrailFetchState

    data class Error(
        val message: String,
    ) : TrailFetchState

    data class UpdateError(
        val message: String,
    ) : TrailFetchState
}
