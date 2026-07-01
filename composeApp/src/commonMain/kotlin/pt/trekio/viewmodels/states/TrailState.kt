package pt.trekio.viewmodels.states

import pt.trekio.dto.ResultIdDto

sealed interface TrailState {
    data object Idle : TrailState

    data object Loading : TrailState

    data class Created(
        val trailId: ResultIdDto,
    ) : TrailState

    data object Deleted : TrailState

    data class Error(
        val message: String,
    ) : TrailState
}
