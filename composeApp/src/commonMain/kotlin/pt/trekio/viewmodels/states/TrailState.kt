package pt.trekio.viewmodels.states

import pt.trekio.dto.ResultIdDto
import pt.trekio.dto.TrailDto

sealed interface TrailState {
    data object Idle : TrailState

    data object Loading : TrailState

    data class Created(
        val trailId: ResultIdDto,
    ) : TrailState

    data class Details(
        val trail: TrailDto,
    ) : TrailState

    data object Deleted : TrailState

    data class Error(
        val message: String,
    ) : TrailState
}
