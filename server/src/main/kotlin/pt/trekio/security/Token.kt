package pt.trekio.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.security.SecureRandom
import java.util.Base64.getUrlEncoder
import java.util.Date
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

object Token {
    private val secret: String = System.getenv("TREKIO_SECRET_JWT")
    val algorithm = Algorithm.HMAC256(secret)!!
    const val TOKEN_BYTES = 256 / 8
    const val MAX_TOKENS = 1

    private val ACCESS_TOKEN_LIFETIME = /* System.getenv("TREKIO_ACCESS_TOKEN_LIFETIME")?.toLong()?.seconds ?: */ 15.minutes
    val REFRESH_TOKEN_LIFETIME = 90.days

    fun generateAccessToken(username: String): String {
        val expiresAt = Date(System.currentTimeMillis() * 1000 + ACCESS_TOKEN_LIFETIME.inWholeSeconds)
        return JWT
            .create()
            .withClaim("username", username)
            .withExpiresAt(expiresAt)
            .sign(algorithm)
    }

    fun generateRefreshToken(): String =
        ByteArray(TOKEN_BYTES).let { byteArray ->
            SecureRandom.getInstanceStrong().nextBytes(byteArray)
            getUrlEncoder().encodeToString(byteArray)
        }
}
