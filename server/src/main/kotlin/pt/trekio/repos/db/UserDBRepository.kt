package pt.trekio.repos.db

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.r2dbc.deleteAll
import org.jetbrains.exposed.v1.r2dbc.deleteReturning
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.insertReturning
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import pt.trekio.domain.User
import pt.trekio.errors.DomainError
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Email
import pt.trekio.misc.OAuthCode
import pt.trekio.misc.Password
import pt.trekio.misc.Token
import pt.trekio.misc.Username
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.UserRepository
import pt.trekio.repos.db.exposed.OAuthCodes
import pt.trekio.repos.db.exposed.Tokens
import pt.trekio.repos.db.exposed.Users
import pt.trekio.security.PasswordEncoder
import kotlin.time.Clock
import kotlin.time.Instant

class UserDBRepository : UserRepository() {
    private companion object {
        fun ResultRow.toUser() =
            User(
                this[Users.id].value,
                Username(this[Users.username]),
                Email(this[Users.email]),
                this[Users.passwordValidation],
                this[Users.rank],
            )

        fun ResultRow.toToken() =
            Token(
                this[Tokens.uid].value,
                this[Tokens.tokenValidation],
                Instant.fromEpochSeconds(
                    this[Tokens.expiredAt],
                ),
            )

        fun ResultRow.toOAuthCode() =
            OAuthCode(
                Email(this[OAuthCodes.email]),
                Username(this[OAuthCodes.username]),
                this[OAuthCodes.code],
                Instant.fromEpochSeconds(
                    this[OAuthCodes.expiredAt],
                ),
            )
    }

    override suspend fun createUser(
        name: Username,
        email: Email,
        password: Password?,
    ): Either<DomainError, User> =
        suspendTransaction {
            if (Users.selectAll().where { Users.username eq name.value }.firstOrNull() != null) {
                return@suspendTransaction failure(UserError.UsernameAlreadyExists)
            }

            if (Users.selectAll().where { Users.email eq email.value }.firstOrNull() != null) {
                return@suspendTransaction failure(UserError.EmailAlreadyUsed)
            }

            val passHash = password?.let { PasswordEncoder.encode(it.value) }
            val newUser =
                Users.insertReturning(listOf(Users.id)) {
                    it[Users.username] = name.value
                    it[Users.email] = email.value
                    it[Users.passwordValidation] = passHash
                }

            val uid = newUser.firstOrNull()?.get(Users.id) ?: return@suspendTransaction failure(DomainError.UnexpectedError)
            success(
                User(
                    uid.value,
                    name,
                    email,
                    passHash,
                ),
            )
        }

    override suspend fun getUserById(id: ULong): User? =
        suspendTransaction {
            Users
                .selectAll()
                .where(Users.id eq id)
                .firstOrNull()
                ?.toUser()
        }

    override suspend fun getUserByName(username: Username) =
        suspendTransaction {
            Users
                .selectAll()
                .where(Users.username eq username.value)
                .firstOrNull()
                ?.toUser()
        }

    override suspend fun getUserByEmail(email: Email) =
        suspendTransaction {
            Users
                .selectAll()
                .where(Users.email eq email.value)
                .firstOrNull()
                ?.toUser()
        }

    override suspend fun getUsers(
        skip: Int,
        limit: Int,
    ) = suspendTransaction {
        Users
            .selectAll()
            .offset(skip.toLong())
            .limit(limit)
            .toList()
            .map { it.toUser() }
    }

    override suspend fun updateUser(
        name: Username,
        updatedInfo: User,
    ): Either<UserError, Unit> =
        suspendTransaction {
            if (Users.selectAll().where { Users.username eq updatedInfo.username.value }.firstOrNull() != null) {
                return@suspendTransaction failure(UserError.UsernameAlreadyExists)
            }

            val changes =
                Users.update({ Users.username eq name.value }) {
                    it[username] = updatedInfo.username.value
                    it[email] = updatedInfo.email.value
                    it[passwordValidation] = updatedInfo.passwordValidInfo
                    it[rank] = updatedInfo.rank
                }

            if (changes != 0) {
                success(Unit)
            } else {
                failure(UserError.UserDoesNotExist)
            }
        }

    override suspend fun deleteUser(username: Username): Either<UserError, Unit> =
        suspendTransaction {
            val changes = Users.deleteReturning(listOf(Users.id)) { Users.username eq username.value }.toList()

            if (changes.isNotEmpty()) {
                changes.forEach {
                    Tokens.deleteWhere { Tokens.uid eq it[Users.id].value }
                }

                success(Unit)
            } else {
                failure(UserError.UserDoesNotExist)
            }
        }

    override suspend fun deleteAllUsers(): Unit =
        suspendTransaction {
            Users.deleteAll()
            Tokens.deleteAll()
        }

    override suspend fun getUserByTokenValidationInfo(tokenValidationInfo: String): Pair<User, Token>? =
        suspendTransaction {
            val token =
                Tokens
                    .selectAll()
                    .where { Tokens.tokenValidation eq tokenValidationInfo }
                    .firstOrNull()
                    ?.toToken() ?: return@suspendTransaction null

            if (token.expiredAt < Clock.System.now()) {
                removeTokenByValidationInfo(tokenValidationInfo)
                return@suspendTransaction null
            }

            val user =
                Users
                    .selectAll()
                    .where { Users.id eq token.uid }
                    .firstOrNull()
                    ?.toUser()

            if (user == null) {
                removeTokenByValidationInfo(tokenValidationInfo)
                return@suspendTransaction null
            }

            user to token
        }

    override suspend fun createToken(
        token: Token,
        maxTokens: Int,
    ): Either<DomainError, Unit> =
        suspendTransaction {
            val isUserMissing =
                Users
                    .selectAll()
                    .where { Users.id eq token.uid }
                    .count() < 1

            if (isUserMissing) return@suspendTransaction failure(UserError.UserDoesNotExist)

            val tokenCount =
                Tokens
                    .selectAll()
                    .where { Tokens.uid eq token.uid }
                    .count()

            if (tokenCount >= maxTokens) {
                val rowsToDelete =
                    Tokens
                        .select(Tokens.tokenValidation)
                        .orderBy(Tokens.expiredAt)
                        .limit((tokenCount - maxTokens + 1).toInt())

                Tokens.deleteWhere { Tokens.tokenValidation inSubQuery rowsToDelete }
            }

            val inserts =
                Tokens
                    .insert {
                        it[Tokens.uid] = token.uid
                        it[Tokens.tokenValidation] = token.tokenValidationInfo
                        it[Tokens.expiredAt] = token.expiredAt.epochSeconds
                    }.insertedCount

            if (inserts < 1) {
                failure(DomainError.UnexpectedError)
            } else {
                success(Unit)
            }
        }

    override suspend fun removeTokenByValidationInfo(tokenValidationInfo: String): Int =
        suspendTransaction {
            Tokens.deleteWhere { tokenValidation eq tokenValidationInfo }
        }

    override suspend fun saveOAuthCode(oauthCode: OAuthCode): Either<DomainError, Unit> =
        suspendTransaction {
            val insert =
                OAuthCodes
                    .insert {
                        it[OAuthCodes.email] = oauthCode.email.value
                        it[OAuthCodes.username] = oauthCode.username.value
                        it[OAuthCodes.code] = oauthCode.code
                        it[OAuthCodes.expiredAt] = oauthCode.expiredAt.epochSeconds
                    }.insertedCount

            return@suspendTransaction if (insert < 1) {
                failure(DomainError.UnexpectedError)
            } else {
                success(Unit)
            }
        }

    override suspend fun getOAuthCode(
        email: Email,
        username: Username,
        code: String,
    ): Boolean =
        suspendTransaction {
            val oauthCode =
                OAuthCodes
                    .selectAll()
                    .where(OAuthCodes.code eq code)
                    .firstOrNull()
                    ?.toOAuthCode() ?: return@suspendTransaction false

            OAuthCodes.deleteWhere {
                (OAuthCodes.code eq code) and
                    (OAuthCodes.email eq email.value) and
                    (OAuthCodes.username eq username.value)
            }
            oauthCode.expiredAt >= Clock.System.now()
        }
}
