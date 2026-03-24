package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val username: String,
    val rank: String,
    val completedTrails: Int,
    val totalKmHiked: Double,
    val totalHikingTime: Long,
)
