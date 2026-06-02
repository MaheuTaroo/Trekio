package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatisticsDto(
    val uid: ULong,
    val trails: Int,
    val totalKms: Double,
    val totalTime: Long,
)
