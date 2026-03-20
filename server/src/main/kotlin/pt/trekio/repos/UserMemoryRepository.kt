package pt.trekio.repos

import pt.trekio.dto.User
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Token
import pt.trekio.misc.failure
import pt.trekio.misc.success
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

object UserMemoryRepository: UserRepository {
    private val users = mutableListOf<User>()
    private val tokens = mutableMapOf<String, Token>()
    private val TOKEN_LIFETIME = 24.hours
    private val lock = ReentrantLock()

    private fun tokensFor(username: String) =
        lock.withLock {
            tokens.filterValues { it.username == username }.values.toList()
        }

    private fun isUserMissing(username: String) =
        lock.withLock { users.none { it.username == username } }

    override fun createUser(user: User): Either<UserError, Unit> {
        if (lock.withLock { users.any { user.username == it.username } })
            return failure(UserError.UsernameAlreadyExists)

        lock.withLock {
            users.add(user)
            users.sortBy(User::username)
        }

        return success(Unit)
    }

    override fun getUser(username: String) =
        lock.withLock {
            users.firstOrNull { it.username == username }
        }

    override fun getUsers(skip: Int, limit: Int) =
        lock.withLock { users.drop(skip) }.take(limit)


    override fun updateUser(
        username: String,
        updatedInfo: User
    ): Either<UserError, Unit> {
        val userIdx = users.indexOfFirst { it.username == username }
        if (userIdx < 0) return failure(UserError.UserDoesNotExist)

        lock.withLock {
            users[userIdx] = updatedInfo
        }

        return success(Unit)
    }

    override fun deleteUser(username: String): Either<UserError, Unit> {
        if (lock.withLock { users.any { it.username == username }})
            return failure(UserError.UsernameAlreadyExists)

        lock.withLock {
            users.removeAll { it.username == username }
        }

        return success(Unit)
    }

    override fun deleteAllUsers() {
        lock.withLock(users::clear)
    }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: String): Pair<User, Token>? {
        val token = lock.withLock { tokens[tokenValidationInfo] } ?: return null

        val user = lock.withLock { users.firstOrNull { it.username == token.username } }

        if (user == null) {
            tokens.remove(tokenValidationInfo)
            return null
        }

        return user to token
    }

    override fun createToken(
        token: Token,
        maxTokens: Int
    ): Either<UserError, Unit> {
        if (isUserMissing(token.username))
            return failure(UserError.UserDoesNotExist)

        lock.withLock {
            val userTokens = tokensFor(token.username)
            if (userTokens.size >= maxTokens) {
                userTokens.dropLast(maxTokens + 1).forEach {
                    removeTokenByValidationInfo(it.tokenValidationInfo)
                }
            }
            tokens[token.tokenValidationInfo] = token
        }
        return success(Unit)
    }

    override fun updateTokenLastUsed(
        token: Token,
        now: Instant
    ): Either<UserError, Unit> {
        if (isUserMissing(token.username))
            return failure(UserError.UserDoesNotExist)

        val userToken = tokens[token.tokenValidationInfo] ?: return failure(UserError.TokenDoesNotExist)

        if (now - userToken.lastUsedAt > TOKEN_LIFETIME) {
            lock.withLock { tokens.remove(token.tokenValidationInfo) }
            return failure(UserError.ExpiredToken)
        }

        lock.withLock { tokens[token.tokenValidationInfo] = token.copy(lastUsedAt = now) }
        return success(Unit)
    }

    override fun removeTokenByValidationInfo(tokenValidationInfo: String): Int {
        if (lock.withLock { tokens[tokenValidationInfo] } == null)
            return 0

        lock.withLock { tokens.remove(tokenValidationInfo) }
        return 1
    }
}