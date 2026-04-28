package pt.trekio.misc

data class UserAndToken(
    val username: String,
    val token: String,
    val expiration: Long,
)
