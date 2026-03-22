package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val username: String,
    val email: String?,
    val rank: String,
    val completedTrails: Int,
    val totalKmHiked: Double,
    val totalHikingTime: Long,
)
