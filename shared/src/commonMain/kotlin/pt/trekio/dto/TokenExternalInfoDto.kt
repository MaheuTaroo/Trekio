package pt.trekio.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class TokenExternalInfoDto(
    val tokenValue: String,
    val tokenExpiration: Long,
)
