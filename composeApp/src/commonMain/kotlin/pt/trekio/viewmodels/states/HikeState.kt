package pt.trekio.viewmodels.states

sealed interface HikeState {
    data object Loading : HikeState

    data object Hiking : HikeState

    data class Error(
        val message: String,
    ) : HikeState

    data object Stopped : HikeState
}
