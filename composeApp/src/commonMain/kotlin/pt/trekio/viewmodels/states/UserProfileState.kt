package pt.trekio.viewmodels.states

import pt.trekio.dto.StatisticsDto

sealed interface UserProfileState {
    data object Idle : UserProfileState

    data object Loading : UserProfileState

    data class Success(
        val statistics: StatisticsDto,
    ) : UserProfileState

    data class Error(
        val message: String,
    ) : UserProfileState
}
