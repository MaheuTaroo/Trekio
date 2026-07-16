package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: ULong,
    val username: String,
    val rank: String,
)
