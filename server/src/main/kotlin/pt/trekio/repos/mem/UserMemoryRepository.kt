package pt.trekio.repos.mem

import pt.trekio.domain.User
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Token
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.UserRepository
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Instant

object UserMemoryRepository : UserRepository() {
    private val users = mutableListOf<User>()
    private val tokens = mutableMapOf<String, Token>()
    private val lock = ReentrantLock()

    /**
     * Retrieves all tokens for a user.
     * THIS FUNCTION IS NOT THREAD-SAFE!
     * @param username The user's name
     * @return A list of the user's tokens
     */
    private fun tokensFor(username: String) = tokens.filterValues { it.username == username }.values.toList()

    /**
     * Checks if a user does not exist.
     * THIS FUNCTION IS NOT THREAD-SAFE!
     * @param username The user's name
     * @return Whether the user is missing or not
     */
    private fun isUserMissing(username: String) = users.none { it.username == username }

    override fun createUser(
        name: String,
        email: String,
        passHash: String,
    ): Either<UserError, User> =
        lock.withLock {
            if (users.any { it.username == name }) {
                return failure(UserError.UsernameAlreadyExists)
            }

            val user = User(name, email, passHash)
            users.add(user)
            users.sortBy(User::username)

            return success(user)
        }

    override fun getUserByName(username: String) =
        lock.withLock {
            users.firstOrNull { it.username == username }
        }

    override fun getUserByEmail(email: String) =
        lock.withLock {
            users.firstOrNull { it.email == email }
        }

    override fun getUsers(
        skip: Int,
        limit: Int,
    ) = lock.withLock { users.drop(skip) }.take(limit)

    override fun updateUser(
        name: String,
        updatedInfo: User,
    ): Either<UserError, Unit> =
        lock.withLock {
            val userIdx = users.indexOfFirst { it.username == name }
            if (userIdx < 0) return failure(UserError.UserDoesNotExist)

            users[userIdx] = updatedInfo

            return success(Unit)
        }

    override fun deleteUser(username: String): Either<UserError, Unit> =
        lock.withLock {
            if (users.none { it.username == username }) {
                return failure(UserError.UserDoesNotExist)
            }

            users.removeAll { it.username == username }
            tokensFor(username).forEach {
                tokens.remove(it.tokenValidationInfo)
            }

            return success(Unit)
        }

    override fun deleteAllUsers() {
        lock.withLock {
            users.clear()
            tokens.clear()
        }
    }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: String): Pair<User, Token>? =
        lock.withLock {
            val token = tokens[tokenValidationInfo] ?: return null

            val user = users.firstOrNull { it.username == token.username }

            if (user == null) {
                tokens.remove(tokenValidationInfo)
                return null
            }

            return user to token
        }

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ): Either<UserError, Unit> =
        lock.withLock {
            if (isUserMissing(token.username)) {
                return failure(UserError.UserDoesNotExist)
            }

            val userTokens = tokensFor(token.username)
            if (userTokens.size >= maxTokens) {
                userTokens.dropLast(maxTokens + 1).forEach {
                    removeTokenByValidationInfo(it.tokenValidationInfo)
                }
            }
            tokens[token.tokenValidationInfo] = token
            return success(Unit)
        }

    override fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ): Either<UserError, Unit> =
        lock.withLock {
            if (isUserMissing(token.username)) {
                return failure(UserError.UserDoesNotExist)
            }

            val userToken = tokens[token.tokenValidationInfo] ?: return failure(UserError.TokenDoesNotExist)

            if (now - userToken.lastUsedAt > tokenLifetime) {
                tokens.remove(token.tokenValidationInfo)
                return failure(UserError.ExpiredToken)
            }

            tokens[token.tokenValidationInfo] = token.copy(lastUsedAt = now)
            return success(Unit)
        }

    override fun removeTokenByValidationInfo(tokenValidationInfo: String): Int =
        lock.withLock {
            if (tokens[tokenValidationInfo] == null) {
                return 0
            }

            tokens.remove(tokenValidationInfo)
            return 1
        }
}
