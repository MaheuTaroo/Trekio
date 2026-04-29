package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class TokenExternalInfoDto(
    val accessTokenValue: String,
    val refreshTokenValue: String,
    val tokenExpiration: Long,
)
