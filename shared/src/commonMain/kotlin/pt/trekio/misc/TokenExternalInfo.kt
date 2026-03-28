package pt.trekio.misc

import pt.trekio.dto.TokenExternalInfoDto
import kotlin.time.Instant

data class TokenExternalInfo(
    val tokenValue: String,
    val tokenExpiration: Instant,
)

fun TokenExternalInfo.toDto() = TokenExternalInfoDto(tokenValue, tokenExpiration.epochSeconds)
