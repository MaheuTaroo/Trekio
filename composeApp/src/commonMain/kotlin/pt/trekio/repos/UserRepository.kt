package pt.trekio.repos

import pt.trekio.misc.UserAndToken

interface UserRepository {
    suspend fun saveToken(
        accessToken: String,
        refreshToken: String,
        expiration: Long,
        email: String? = null,
    )

    suspend fun getTokens(): UserAndToken?

    suspend fun clear()
}
