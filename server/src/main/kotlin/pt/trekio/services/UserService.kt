package pt.trekio.services

import pt.trekio.domain.User
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Failure
import pt.trekio.misc.Token
import pt.trekio.misc.TokenExternalInfo
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.UserRepository
import java.security.SecureRandom
import java.util.Base64.getUrlEncoder
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

class UserService(
    private val repo: UserRepository,
) {
    private companion object {
        val LETTERS = 'a'..'z' union 'A'..'Z'
        val DIGITS = '0'..'9'
        const val SYMBOLS = """!"#$%&/()=?»@£§€{[]}«+*-<>\|;:_"""
        const val TOKEN_BYTES = 256 / 8
        val TOKEN_LIFETIME = 24.hours
        const val MAX_TOKENS = 1

        private fun generateTokenValue(): String =
            ByteArray(TOKEN_BYTES).let { byteArray ->
                SecureRandom.getInstanceStrong().nextBytes(byteArray)
                getUrlEncoder().encodeToString(byteArray)
            }

        fun String.isValidEmail(): Boolean {
            var atFlag = false

            forEach {
                if (it == '@') {
                    if (atFlag) return false
                    atFlag = true
                }
            }
            if (!atFlag) return false
            if (endsWith('@') || ('.' in this && endsWith('.'))) return false

            return true
        }
    }

    fun getUsers(
        skip: Int,
        limit: Int,
    ): Either<UserError, List<User>> {
        if (skip < 0) return failure(UserError.NegativeSkip)
        if (limit < 1) return failure(UserError.NonPositiveLimit)

        return success(repo.getUsers(skip, limit))
    }

    fun createUser(
        username: String,
        email: String,
        password: String,
    ): Either<UserError, TokenExternalInfo> {
        if (repo.getUserByName(username) != null) {
            return failure(UserError.UsernameAlreadyExists)
        }

        if (username.length < 3 || username[0] !in LETTERS) {
            return failure(UserError.InvalidUsername)
        }

        if (!email.isValidEmail()) {
            return failure(UserError.InvalidEmail)
        }

        if (repo.getUserByEmail(email) != null) {
            return failure(UserError.EmailAlreadyUsed)
        }

        if (password.length < 8 ||
            password.any { it !in (LETTERS union DIGITS) + SYMBOLS || it == ' ' }
        ) {
            return failure(UserError.InvalidPassword)
        }

        val userRes = repo.createUser(username, email, password)
        if (userRes is Failure) {
            return userRes
        }

        return createTokenFor(username, password)
    }

    fun getOwnDetails(token: String): Either<UserError, User> {
        val (user, _) = repo.getTokenByTokenValidationInfo(token) ?: return failure(UserError.InvalidToken)

        return success(user)
    }

    fun getUser(username: String): Either<UserError, User> {
        val user = repo.getUserByName(username) ?: return failure(UserError.UserDoesNotExist)

        return success(user)
    }

    fun deleteUser(token: String): Either<UserError, Unit> {
        val (user, _) = repo.getTokenByTokenValidationInfo(token) ?: return failure(UserError.InvalidToken)

        return repo.deleteUser(user.username)
    }

    fun createTokenFor(
        username: String,
        password: String,
    ): Either<UserError, TokenExternalInfo> {
        val user = repo.getUserByName(username) ?: return failure(UserError.UserDoesNotExist)
        if (user.passwordValidInfo != password) return failure(UserError.IncorrectPassword)

        val generatedToken = generateTokenValue()
        val token =
            Token(
                username,
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
