package pt.trekio.domain

import pt.trekio.dto.StatisticsDto

data class Statistics(
    val userId: ULong,
    val completedTrails: Int,
    val totalKilometersHiked: Double,
    val totalHikingTime: Long,
)

fun Statistics.toDto() =
    StatisticsDto(
        userId,
        completedTrails,
        totalKilometersHiked,
        totalHikingTime
    )