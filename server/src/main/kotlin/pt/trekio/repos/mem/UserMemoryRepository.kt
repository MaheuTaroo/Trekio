package pt.trekio.repos.mem

import pt.trekio.domain.User
import pt.trekio.errors.DomainError
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Email
import pt.trekio.misc.Password
import pt.trekio.misc.Token
import pt.trekio.misc.Username
import pt.trekio.misc.failure
import pt.trekio.misc.hash
import pt.trekio.misc.success
import pt.trekio.repos.contracts.UserRepository
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Instant

object UserMemoryRepository : UserRepository() {
    private val users = mutableMapOf<ULong, User>()
    private val tokens = mutableMapOf<String, Token>()
    private val lock = ReentrantLock()

    private var userCount = 1uL

    /**
     * Retrieves all tokens for a user.
     * THIS FUNCTION IS NOT THREAD-SAFE!
     * @param uid The user's internal ID
     * @return A list of the user's tokens
     */
    private fun tokensFor(uid: ULong) = tokens.filterValues { it.uid == uid }.values.toList()

    override fun createUser(
        name: Username,
        email: Email,
        password: Password,
    ): Either<DomainError, User> =
        lock.withLock {
            if (users.values.any { it.username == name }) {
                return failure(UserError.UsernameAlreadyExists)
            }

            if (users.values.any { it.email == email }) {
                return failure(UserError.EmailAlreadyUsed)
            }

            val user =
                User(
                    userCount,
                    name,
                    email,
                    password.hash(),
                )
            users[userCount++] = user

            return success(user)
        }

    override fun getUserById(id: ULong) =
        lock.withLock {
            users[id]
        }

    override fun getUserByName(username: Username) =
        lock.withLock {
            users.values.firstOrNull { it.username == username }
        }

    override fun getUserByEmail(email: Email) =
        lock.withLock {
            users.values.firstOrNull { it.email == email }
        }

    override fun getUsers(
        skip: Int,
        limit: Int,
    ) = lock.withLock { users.values.drop(skip) }.take(limit)

    override fun updateUser(
        name: Username,
        updatedInfo: User,
    ): Either<UserError, Unit> =
        lock.withLock {
            val userIdx = users.values.firstOrNull { it.username == name }?.id ?: return failure(UserError.UserDoesNotExist)

            users[userIdx] = updatedInfo

            return success(Unit)
        }

    override fun deleteUser(username: Username): Either<UserError, Unit> =
        lock.withLock {
            val user =
                users.values.firstOrNull { it.username == username }
                    ?: return failure(UserError.UserDoesNotExist)

            users.remove(user.id)
            tokensFor(user.id).forEach {
                tokens.remove(it.tokenValidationInfo)
            }

            return success(Unit)
        }

    override fun deleteAllUsers() {
        lock.withLock {
            users.clear()
            tokens.clear()
            userCount = 1uL
        }
    }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: String): Pair<User, Token>? =
        lock.withLock {
            val token = tokens[tokenValidationInfo] ?: return null

            val user = users[token.uid]

            if (user == null) {
                tokens.remove(tokenValidationInfo)
                return null
            }

            return user to token
        }

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ): Either<DomainError, Unit> =
        lock.withLock {
            if (users[token.uid] == null) {
                return failure(UserError.UserDoesNotExist)
            }

            val userTokens = tokensFor(token.uid)
            if (userTokens.size >= maxTokens) {
                userTokens.take(maxTokens).forEach {
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
            if (users[token.uid] == null) {
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
