package pt.trekio.services

import pt.trekio.misc.UserAndToken
import pt.trekio.misc.UserDetailsAndToken
import pt.trekio.repos.UserRepository
import pt.trekio.services.utils.TestUser.ACCESS_TOKEN
import pt.trekio.services.utils.TestUser.RANK
import pt.trekio.services.utils.TestUser.UID
import pt.trekio.services.utils.TestUser.USERNAME

object SuccessfulUserRepo : UserRepository {
    var id: ULong? = UID
        private set
    var username: String? = USERNAME
        private set
    var email: String? = null
        private set
    var rank: String? = RANK
        private set

    var accessToken: String? = ACCESS_TOKEN
        private set
    var refreshToken: String? = null
        private set
    var expiration: Long? = null
        private set

    override suspend fun saveToken(
        accessToken: String,
        refreshToken: String,
        expiration: Long,
        email: String?,
    ) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.expiration = expiration
        email?.let { this.email = it }
    }

    override suspend fun getTokens(): UserAndToken? {
        val token = accessToken ?: return null
        val refresh = refreshToken ?: return null
        val expirationValue = expiration ?: return null
        val userEmail = email ?: return null
        return UserAndToken(
            accessToken = token,
            refreshToken = refresh,
            expiration = expirationValue,
            email = userEmail,
        )
    }

    override suspend fun saveOwnDetails(
        id: ULong?,
        username: String,
        rank: String?,
    ) {
        id?.let { this.id = it }
        this.username = username
        rank?.let { this.rank = it }
    }

    override suspend fun getOwnDetails(): UserDetailsAndToken? {
        val token = accessToken ?: return null
        val userId = id ?: return null
        val userName = username ?: return null
        val userRank = rank ?: return null
        return UserDetailsAndToken(
            id = userId,
            username = userName,
            rank = userRank,
            accessToken = token,
        )
    }

    override suspend fun clear() {
        id = null
        username = null
        email = null
        rank = null

        accessToken = null
        refreshToken = null
        expiration = null
    }

    fun isClear(): Boolean =
        id == null && username == null && email == null && rank == null && accessToken == null && refreshToken == null && expiration == null
}
