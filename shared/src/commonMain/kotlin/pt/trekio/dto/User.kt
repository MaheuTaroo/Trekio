package pt.trekio.dto

import kotlinx.serialization.Serializable

enum class UserRank {
    NEW,
    VERIFIED
}

@Serializable
data class User(
    val username: String,
    val email: String?,
    val passwordValidInfo: String,
    val rank: UserRank,
    val completedTrails: Int,
    val totalKmHiked: Double,
    val totalHikingTime: Long,
)
