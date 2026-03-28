package pt.trekio.misc

import kotlin.time.Instant

data class Token(
    val uid: ULong,
    val tokenValidationInfo: String,
    val lastUsedAt: Instant,
)
