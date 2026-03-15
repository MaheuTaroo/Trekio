package pt.trekio.repos

import pt.trekio.dto.User
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Token
import pt.trekio.misc.failure
import pt.trekio.misc.success
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Instant

object UserMemoryRepository: UserRepository {
    private val users = mutableListOf<User>()
    private val tokens = mutableMapOf<String, String>()

    private val lock = ReentrantLock()

    override fun createUser(user: User): Either<UserError, Unit> {
        if (lock.withLock { users.any { user.username == it.username } })
            return failure(UserError.UsernameAlreadyExists)

        lock.withLock {
            users.add(user)
            users.sortBy(User::username)
        }

        return success(Unit)
    }

    override fun getUser(username: String) = users.firstOrNull { it.username == username }

    override fun getUsers(skip: Int, limit: Int) =
        users.drop(skip).take(limit)

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

        users.removeAll { it.username == username }

        return success(Unit)
    }

    override fun deleteAllUsers() {
        users.clear()
    }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: String): Pair<User, Token>? {
        TODO("Not yet implemented")
    }

    override fun createToken(
        token: Token,
        maxTokens: Int
    ): Either<UserError, Unit> {
        TODO("Not yet implemented")
    }

    override fun updateTokenLastUsed(
        token: Token,
        now: Instant
    ): Either<UserError, Unit> {
        TODO("Not yet implemented")
    }

    override fun removeTokenByValidationInfo(tokenValidationInfo: String): Int {
        TODO("Not yet implemented")
    }
}