package pt.trekio.services

import pt.trekio.domain.User
import pt.trekio.errors.DomainError
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Email
import pt.trekio.misc.Failure
import pt.trekio.misc.OAuthCode
import pt.trekio.misc.Password
import pt.trekio.misc.Token
import pt.trekio.misc.TokenExternalInfo
import pt.trekio.misc.Username
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.UserRepository
import pt.trekio.security.PasswordEncoder
import pt.trekio.security.Sha256TokenEncoder.createValidationInformation
import pt.trekio.security.Token.MAX_TOKENS
import pt.trekio.security.Token.REFRESH_TOKEN_LIFETIME
import pt.trekio.security.Token.generateAccessToken
import pt.trekio.security.Token.generateRefreshToken
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class UserService(
    private val repo: UserRepository,
) : Service() {
    suspend fun getUsers(
        skip: Int,
        limit: Int,
    ): Either<DomainError, List<User>> = paginated(skip, limit, repo::getUsers)

    suspend fun createUser(
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

    suspend fun getUserById(userId: ULong): Either<UserError, User> {
        val user = repo.getUserById(userId) ?: return failure(UserError.UserDoesNotExist)

        return success(user)
    }

    suspend fun getUser(username: String): Either<UserError, User> {
        var name: Username

        try {
            name = Username(username)
        } catch (e: IllegalArgumentException) {
            return failure(UserError.InvalidUsername(e.message ?: "Invalid username"))
        }

        val user = repo.getUserByName(name) ?: return failure(UserError.UserDoesNotExist)

        return success(user)
    }

    suspend fun deleteUser(token: String): Either<UserError, Unit> {
        // Supposed to never reach failure
        val (user, _) = repo.getUserByTokenValidationInfo(token) ?: return failure(UserError.InvalidToken)

        return repo.deleteUser(user.username)
    }

    suspend fun getUserByToken(token: String): Either<UserError, User> {
        val (user, _) = repo.getUserByTokenValidationInfo(token) ?: return failure(UserError.InvalidToken)

        return success(user)
    }

    suspend fun updateUser(
        username: String?,
        password: String?,
        userId: ULong,
    ): Either<DomainError, TokenExternalInfo> {
        val user = repo.getUserById(userId) ?: return failure(UserError.InvalidToken)

        if (username == null && password == null) return refreshToken(userId)

        var name: Username = user.username
        var pass: Password? = null

        if (username != null) {
            try {
                name = Username(username)
            } catch (e: IllegalArgumentException) {
                return failure(UserError.InvalidUsername(e.message ?: "Invalid username"))
            }

            if (repo.getUserByName(name) != null) {
                return failure(UserError.UsernameAlreadyExists)
            }
        }

        if (password != null) {
            try {
                pass = Password(password)
            } catch (e: IllegalArgumentException) {
                return failure(UserError.InvalidPassword(e.message ?: "Invalid password"))
            }
        }

        val res =
            repo.updateUser(
                user.username,
                user.copy(
                    username = name,
                    passwordValidInfo = pass?.let { PasswordEncoder.encode(it.value) } ?: user.passwordValidInfo,
                ),
            )
        if (res is Failure) return res

        return refreshToken(userId)
    }

    suspend fun createTokenFor(
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

        if (user.passwordValidInfo == null) return failure(UserError.EmailUsedInOAuth)

        val passwordMatch = PasswordEncoder.matches(password, user.passwordValidInfo!!)
        if (!passwordMatch) return failure(UserError.IncorrectPassword)

        return refreshToken(user.id)
    }

    suspend fun refreshToken(userId: ULong): Either<DomainError, TokenExternalInfo> {
        val user = repo.getUserById(userId) ?: return failure(UserError.InvalidToken)
        val accessToken = generateAccessToken(user.username.value)
        val refreshToken = generateRefreshToken()
        val hashedRefreshToken =
            Token(
                userId,
                createValidationInformation(refreshToken),
                Clock.System.now() + REFRESH_TOKEN_LIFETIME,
            )
        val res = repo.createToken(hashedRefreshToken, MAX_TOKENS)
        if (res is Failure) return res
        return success(
            TokenExternalInfo(
                accessToken,
                refreshToken,
                hashedRefreshToken.expiredAt,
            ),
        )
    }

    suspend fun revokeToken(refreshToken: String): Either<UserError, Unit> {
        repo.getUserByTokenValidationInfo(refreshToken) ?: return failure(UserError.InvalidToken)

        val removals = repo.removeTokenByValidationInfo(refreshToken)
        return if (removals > 0) success(Unit) else failure(UserError.InvalidToken)
    }

    suspend fun oauthService(email: String): Either<DomainError, Pair<OAuthCode, Boolean>> {
        var mail: Email
        try {
            mail = Email(email)
        } catch (e: IllegalArgumentException) {
            return failure(UserError.InvalidEmail(e.message ?: "Invalid email"))
        }

        val user = repo.getUserByEmail(mail)
        var username: Username
        if (user == null) {
            do {
                username =
                    try {
                        Username(email.split('@', limit = 2).first())
                    } catch (_: IllegalArgumentException) {
                        Username.generateRandomName()
                    }
                val res = repo.createUser(username, mail, null)
            } while (res is Failure)
        } else {
            username = user.username
        }

        val oauthCode = OAuthCode(mail, username, generateRefreshToken(), Clock.System.now() + 2.minutes)
        val res = repo.saveOAuthCode(oauthCode)
        if (res is Failure) return res
        return success(Pair(oauthCode, user == null))
    }

    suspend fun oauthVerifyCode(
        email: String,
        username: String,
        code: String,
    ): Either<DomainError, TokenExternalInfo> {
        var name: Username
        var mail: Email

        try {
            name = Username(username)
        } catch (e: IllegalArgumentException) {
            return failure(UserError.InvalidUsername(e.message ?: "Invalid username"))
        }

        try {
            mail = Email(email)
        } catch (e: IllegalArgumentException) {
            return failure(UserError.InvalidEmail(e.message ?: "Invalid email"))
        }

        if (repo.getOAuthCode(mail, name, code)) {
            val user = repo.getUserByEmail(mail) ?: return failure(UserError.InvalidToken)
            return refreshToken(user.id)
        } else {
            return failure(UserError.InvalidCode)
        }
    }
}
