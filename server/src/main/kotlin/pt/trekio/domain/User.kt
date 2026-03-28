package pt.trekio.domain

import pt.trekio.dto.UserDto
import pt.trekio.misc.UserRank

data class User(
    val id: ULong,
    val username: String,
    val email: String,
    val passwordValidInfo: String,
    val rank: UserRank = UserRank.NEW,
    val completedTrails: Int = 0,
    val totalKmHiked: Double = 0.0,
    val totalHikingTime: Long = 0,
)

fun User.toDto() = UserDto(username, rank.name, completedTrails, totalKmHiked, totalHikingTime)
