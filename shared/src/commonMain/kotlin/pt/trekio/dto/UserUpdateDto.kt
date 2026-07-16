package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserUpdateDto(
    val username: String? = null,
    val password: String? = null,
)
