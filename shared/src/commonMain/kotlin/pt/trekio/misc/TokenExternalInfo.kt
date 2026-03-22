package pt.trekio.misc

import kotlin.time.Instant

data class TokenExternalInfo(
    val tokenValue: String,
    val tokenExpiration: Instant,
)
