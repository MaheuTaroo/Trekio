package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class TokenExternalInfoDto(
    val tokenValue: String,
    val tokenExpiration: Long,
)
