package pt.trekio.repos

import pt.trekio.misc.UserAndToken
import pt.trekio.misc.UserDetailsAndToken

interface UserRepository {
    suspend fun saveToken(
        accessToken: String,
        refreshToken: String,
        expiration: Long,
        email: String? = null,
    )

    suspend fun getTokens(): UserAndToken?

    suspend fun saveOwnDetails(
        id: ULong,
        username: String,
        rank: String,
    )

    suspend fun getOwnDetails(): UserDetailsAndToken?

    suspend fun clear()
}
