package pt.trekio.viewmodels.states

sealed interface HikeState {
    data object Loading : HikeState

    data object Hiking : HikeState

    data class Error(
        val message: String,
    ) : HikeState

    data object AboutToCancel : HikeState

    data object AboutToFinish : HikeState

    data object Stopping : HikeState

    data class Details(
        val start: Long,
        val finish: Long,
        val distance: Double,
    ) : HikeState

    data object Stopped : HikeState
}
