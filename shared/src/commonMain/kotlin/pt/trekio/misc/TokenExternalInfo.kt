package pt.trekio.misc

import pt.trekio.dto.TokenExternalInfoDto
import kotlin.time.Instant

data class TokenExternalInfo(
    val accessTokenValue: String,
    val refreshTokenValue: String,
    val tokenExpiration: Instant,
)

fun TokenExternalInfo.toDto() = TokenExternalInfoDto(accessTokenValue, refreshTokenValue, tokenExpiration.epochSeconds)
