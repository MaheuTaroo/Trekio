package pt.trekio.misc

import kotlin.time.Instant

data class Token(
    val username: String,
    val tokenValidationInfo: String,
    val lastUsedAt: Instant,
)