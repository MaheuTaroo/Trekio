package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class GoogleUserResponse(
    val name: String,
    val email: String,
)
