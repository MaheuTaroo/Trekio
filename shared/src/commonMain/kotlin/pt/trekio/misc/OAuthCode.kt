package pt.trekio.misc

import pt.trekio.dto.OAuthCodeDto
import kotlin.time.Instant

data class OAuthCode(
    val email: Email,
    val username: Username,
    val code: String,
    val expiredAt: Instant,
)

fun OAuthCode.toOAuthCodeDto() = OAuthCodeDto(email.value, username.value, code)
