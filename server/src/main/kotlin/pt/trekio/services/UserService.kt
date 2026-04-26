package pt.trekio.services

import pt.trekio.domain.User
import pt.trekio.errors.DomainError
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Email
import pt.trekio.misc.Failure
import pt.trekio.misc.Password
import pt.trekio.misc.Token
import pt.trekio.misc.TokenExternalInfo
import pt.trekio.misc.Username
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.UserRepository
import java.security.SecureRandom
import java.util.Base64.getUrlEncoder
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

class UserService(
    private val repo: UserRepository,
) : Service() {
    private companion object {
        const val TOKEN_BYTES = 256 / 8
        val TOKEN_LIFETIME = 24.hours
        const val MAX_TOKENS = 1

        private fun generateTokenValue(): String =
            ByteArray(TOKEN_BYTES).let { byteArray ->
                SecureRandom.getInstanceStrong().nextBytes(byteArray)
                getUrlEncoder().encodeToString(byteArray)
            }
    }

    fun getUsers(
        skip: Int,
        limit: Int,
    ): Either<DomainError, List<User>> = paginated(skip, limit, repo::getUsers)

    fun createUser(
        username: String,
        email: String,
        password: String,
    ): Either<DomainError, TokenExternalInfo> {
        var name: Username
        var mail: Email
        var pass: Password

        try {
            name = Username(username)
        } catch (e: IllegalArgumentException) {
            return failure(UserError.InvalidUsername(e.message ?: "Invalid username"))
        }

        if (repo.getUserByName(name) != null) {
            return failure(UserError.UsernameAlreadyExists)
        }

        try {
            mail = Email(email)
        } catch (e: IllegalArgumentException) {
            return failure(UserError.InvalidEmail(e.message ?: "Invalid email"))
        }

        if (repo.getUserByEmail(mail) != null) {
            return failure(UserError.EmailAlreadyUsed)
        }

        try {
            pass = Password(password)
        } catch (e: IllegalArgumentException) {
            return failure(UserError.InvalidPassword(e.message ?: "Invalid password"))
        }

        val userRes = repo.createUser(name, mail, pass)
        if (userRes is Failure) {
            return userRes
        }

        return createTokenFor(email, password)
    }

    fun getOwnDetails(token: String): Either<UserError, User> {
        // Supposed to never reach failure
        val (user, _) = repo.getTokenByTokenValidationInfo(token) ?: return failure(UserError.InvalidToken)

        return success(user)
    }

    fun getUser(username: String): Either<UserError, User> {
        var name: Username

        try {
            name = Username(username)
        } catch (e: IllegalArgumentException) {
            return failure(UserError.InvalidUsername(e.message ?: "Invalid username"))
        }

        val user = repo.getUserByName(name) ?: return failure(UserError.UserDoesNotExist)

        return success(user)
    }

    fun deleteUser(token: String): Either<UserError, Unit> {
        // Supposed to never reach failure
        val (user, _) = repo.getTokenByTokenValidationInfo(token) ?: return failure(UserError.InvalidToken)

        return repo.deleteUser(user.username)
    }

    fun createTokenFor(
        email: String,
        password: String,
    ): Either<DomainError, TokenExternalInfo> {
        var mail: Email

        try {
            mail = Email(email)
        } catch (e: IllegalArgumentException) {
            return failure(UserError.InvalidEmail(e.message ?: "Invalid email"))
        }

        val user = repo.getUserByEmail(mail) ?: return failure(UserError.UserDoesNotExist)
        if (user.passwordValidInfo != password) return failure(UserError.IncorrectPassword)

        val generatedToken = generateTokenValue()
        val token =
            Token(
                user.id,
                generatedToken,
                Clock.System.now() + TOKEN_LIFETIME,
            )
        val res = repo.createToken(token, MAX_TOKENS)
        if (res is Failure) return res
        return success(
            TokenExternalInfo(
                generatedToken,
                token.lastUsedAt + TOKEN_LIFETIME,
            ),
        )
    }

    fun revokeToken(token: String): Either<UserError, Unit> {
        repo.getTokenByTokenValidationInfo(token) ?: return failure(UserError.InvalidToken)

        val removals = repo.removeTokenByValidationInfo(token)
        return if (removals > 0) success(Unit) else failure(UserError.ExpiredToken)
    }
}
