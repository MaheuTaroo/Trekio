package pt.trekio.misc

data class UserAndToken(
    val accessToken: String,
    val refreshToken: String,
    val expiration: Long,
    val email: String?,
)
