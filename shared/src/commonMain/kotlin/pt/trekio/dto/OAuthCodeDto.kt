package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class OAuthCodeDto(
    val email: String,
    val username: String,
    val code: String,
)
