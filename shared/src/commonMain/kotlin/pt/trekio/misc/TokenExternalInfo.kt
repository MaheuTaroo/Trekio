package pt.trekio.misc

import pt.trekio.dto.TokenExternalInfoDto
import kotlin.time.Instant

data class TokenExternalInfo(
    val tokenValue: String,
    val tokenExpiration: Instant,
) {
    fun toTokenExternalInfo() = TokenExternalInfoDto(tokenValue, tokenExpiration)
}
