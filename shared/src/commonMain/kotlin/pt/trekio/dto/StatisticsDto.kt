package pt.trekio.dto

data class StatisticsDto(
    val uid: ULong,
    val trails: Int,
    val totalKms: Double,
    val totalTime: Long,
)