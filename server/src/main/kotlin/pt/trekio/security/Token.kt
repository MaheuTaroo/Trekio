package pt.trekio.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import pt.trekio.misc.Routes.BASE_URL
import java.security.SecureRandom
import java.util.Base64.getUrlEncoder
import java.util.Date
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object Token {
    private val secret: String =
        requireNotNull(System.getenv("TREKIO_SECRET_JWT")) { "JWT secret missing" }
    val algorithm = requireNotNull(Algorithm.HMAC256(secret)) { "Algorithm needs to be cannot be null" }
    const val TOKEN_BYTES = 256 / 8
    const val MAX_TOKENS = 1

    private val ACCESS_TOKEN_LIFETIME = System.getenv("TREKIO_ACCESS_TOKEN_LIFETIME")?.toLong()?.seconds ?: 15.minutes
    val REFRESH_TOKEN_LIFETIME = 90.days

    fun generateAccessToken(username: String): String {
        val expiresAt = Date(System.currentTimeMillis() + ACCESS_TOKEN_LIFETIME.inWholeMilliseconds)
        return JWT
            .create()
            .withIssuer(BASE_URL)
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
