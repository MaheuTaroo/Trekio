package pt.trekio.viewmodels.states

import pt.trekio.dto.TrailDto
import pt.trekio.dto.TrailListDto

sealed interface TrailFetchState {
    data object Idle : TrailFetchState

    data object Loading : TrailFetchState

    data class Success(val trails: List<TrailDto>) : TrailFetchState

    data class Error(val message: String) : TrailFetchState
}