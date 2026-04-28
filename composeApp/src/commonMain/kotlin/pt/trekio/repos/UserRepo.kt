package pt.trekio.repos

import pt.trekio.misc.UserAndToken

interface UserRepo {
    suspend fun saveToken(
        token: String,
        expiration: Long,
        email: String? = null,
    )

    suspend fun getToken(): UserAndToken?

    suspend fun clear()
}
