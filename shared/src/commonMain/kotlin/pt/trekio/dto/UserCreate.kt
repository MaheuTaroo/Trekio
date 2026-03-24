package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserCreate(
    val username: String,
    val email: String,
    val password: String,
)
