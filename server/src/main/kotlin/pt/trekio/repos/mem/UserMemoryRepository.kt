package pt.trekio.repos.mem

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pt.trekio.domain.User
import pt.trekio.errors.DomainError
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Email
import pt.trekio.misc.Password
import pt.trekio.misc.Token
import pt.trekio.misc.Username
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.UserRepository
import pt.trekio.security.PasswordEncoder
import kotlin.time.Clock

object UserMemoryRepository : UserRepository() {
    private val users = mutableMapOf<ULong, User>()
    private val tokens = mutableMapOf<String, Token>()
    private val mutex = Mutex()

    private var userCount = 1uL

    /**
     * Retrieves all tokens for a user.
     * THIS FUNCTION IS NOT THREAD-SAFE!
     * @param uid The user's internal ID
     * @return A list of the user's tokens
     */
    private fun tokensFor(uid: ULong) = tokens.filterValues { it.uid == uid }.values.toList()

    override suspend fun createUser(
        name: Username,
        email: Email,
        password: Password?,
    ): Either<DomainError, User> =
        mutex.withLock {
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
                    password?.let { PasswordEncoder.encode(it.value) },
                )
            users[userCount++] = user

            return success(user)
        }

    override suspend fun getUserById(id: ULong) =
        mutex.withLock {
            users[id]
        }

    override suspend fun getUserByName(username: Username) =
        mutex.withLock {
            users.values.firstOrNull { it.username == username }
        }

    override suspend fun getUserByEmail(email: Email) =
        mutex.withLock {
            users.values.firstOrNull { it.email == email }
        }

    override suspend fun getUsers(
        skip: Int,
        limit: Int,
    ) = mutex.withLock { users.values.drop(skip) }.take(limit)

    override suspend fun updateUser(
        name: Username,
        updatedInfo: User,
    ): Either<UserError, Unit> =
        mutex.withLock {
            val userIdx = users.values.firstOrNull { it.username == name }?.id ?: return failure(UserError.UserDoesNotExist)

            if (users.values.any { it.username == updatedInfo.username }) {
                return failure(UserError.UsernameAlreadyExists)
            }

            users[userIdx] = updatedInfo

            return success(Unit)
        }

    override suspend fun deleteUser(username: Username): Either<UserError, Unit> =
        mutex.withLock {
            val user =
                users.values.firstOrNull { it.username == username }
                    ?: return failure(UserError.UserDoesNotExist)

            users.remove(user.id)
            tokensFor(user.id).forEach {
                tokens.remove(it.tokenValidationInfo)
            }

            return success(Unit)
        }

    override suspend fun deleteAllUsers() {
        mutex.withLock {
            users.clear()
            tokens.clear()
            userCount = 1uL
        }
    }

    override suspend fun getUserByTokenValidationInfo(tokenValidationInfo: String): Pair<User, Token>? =
        mutex.withLock {
            val token = tokens[tokenValidationInfo] ?: return null

            if (token.expiredAt < Clock.System.now()) {
                removeTokenWithoutCoroutineSync(tokenValidationInfo)
                return null
            }

            val user = users[token.uid]

            if (user == null) {
                tokens.remove(tokenValidationInfo)
                return null
            }

            return user to token
        }

    override suspend fun createToken(
        token: Token,
        maxTokens: Int,
    ): Either<DomainError, Unit> =
        mutex.withLock {
            if (users[token.uid] == null) {
                return failure(UserError.UserDoesNotExist)
            }

            val userTokens = tokensFor(token.uid)
            if (userTokens.size >= maxTokens) {
                userTokens.take(maxTokens).forEach {
                    removeTokenWithoutCoroutineSync(it.tokenValidationInfo)
                }
            }
            tokens[token.tokenValidationInfo] = token
            return success(Unit)
        }

    /**
     * Needed to avoid deadlock on mutex await when, for example,
     * a user is logging in
     */
    private fun removeTokenWithoutCoroutineSync(tokenValidationInfo: String): Int {
        if (tokens[tokenValidationInfo] == null) {
            return 0
        }

        tokens.remove(tokenValidationInfo)
        return 1
    }

    override suspend fun removeTokenByValidationInfo(tokenValidationInfo: String): Int =
        mutex.withLock {
            removeTokenWithoutCoroutineSync(tokenValidationInfo)
        }
}
