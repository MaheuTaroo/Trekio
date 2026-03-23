package pt.trekio.repos.db

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import pt.trekio.domain.User
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Token
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.UserRepository
import pt.trekio.repos.db.exposed.Tokens
import pt.trekio.repos.db.exposed.Users
import kotlin.time.Clock
import kotlin.time.Instant

class UserDBRepository(
    conn: String,
    user: String,
    password: String,
) : UserRepository() {
    private fun ResultRow.toUser() =
        User(
            this[Users.username],
            this[Users.email],
            this[Users.passwordValidation],
            this[Users.rank],
            this[Users.trails],
            this[Users.totalKms],
            this[Users.hikingTime],
        )

    private fun ResultRow.toToken() =
        Token(
            this[Tokens.username],
            this[Tokens.tokenValidation],
            Instant.fromEpochSeconds(
                this[Tokens.lastUse],
            ),
        )

    init {
        Database.connect(conn, "org.postgresql.ds.PGSimpleDataSource", user, password)
        if (!Users.exists()) {
            transaction {
                Users.ddl.forEach(this::exec)
            }
        }
    }

    override fun createUser(
        name: String,
        email: String,
        passHash: String,
    ): Either<UserError, User> =
        transaction {
            if (Users.select(Users.username).any { it[Users.username] == name }) {
                return@transaction failure(UserError.UsernameAlreadyExists)
            }

            Users.insert {
                it[Users.username] = name
                it[Users.email] = email
                it[Users.passwordValidation] = passHash
            }

            return@transaction success(User(name, email, passHash))
        }

    override fun getUser(username: String) =
        Users
            .selectAll()
            .where(Users.username eq username)
            .firstOrNull()
            ?.toUser()

    override fun getUsers(
        skip: Int,
        limit: Int,
    ) = Users
        .selectAll()
        .offset(skip.toLong())
        .limit(limit)
        .map { it.toUser() }

    override fun updateUser(
        name: String,
        updatedInfo: User,
    ): Either<UserError, Unit> {
        val changes =
            Users.update({ Users.username eq name }) {
                it[username] = updatedInfo.username
                it[email] = updatedInfo.email
                it[passwordValidation] = updatedInfo.passwordValidInfo
                it[rank] = updatedInfo.rank
                it[trails] = updatedInfo.completedTrails
                it[totalKms] = updatedInfo.totalKmHiked
                it[hikingTime] = updatedInfo.totalHikingTime
            }

        return if (changes != 0) {
            success(Unit)
        } else {
            failure(UserError.UserDoesNotExist)
        }
    }

    override fun deleteUser(username: String): Either<UserError, Unit> {
        val changes = Users.deleteWhere { Users.username eq username }

        return if (changes != 0) {
            success(Unit)
        } else {
            failure(UserError.UserDoesNotExist)
        }
    }

    override fun deleteAllUsers() {
        Users.deleteAll()
    }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: String): Pair<User, Token>? =
        transaction {
            val token =
                Tokens
                    .selectAll()
                    .where { Tokens.tokenValidation eq tokenValidationInfo }
                    .firstOrNull()
                    ?.toToken() ?: return@transaction null

            if (token.lastUsedAt < Clock.System.now()) {
                removeTokenByValidationInfo(tokenValidationInfo)
                return@transaction null
            }

            val user =
                Users
                    .selectAll()
                    .where { Users.username eq token.username }
                    .firstOrNull()
                    ?.toUser()

            if (user == null) {
                removeTokenByValidationInfo(tokenValidationInfo)
                return@transaction null
            }

            return@transaction user to token
        }

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ): Either<UserError, Unit> =
        transaction {
            val isUserMissing =
                Users
                    .selectAll()
                    .where { Users.username eq token.username }
                    .count() < 1

            if (isUserMissing) return@transaction failure(UserError.UserDoesNotExist)

            val tokenCount =
                Tokens
                    .selectAll()
                    .where { Tokens.username eq token.username }
                    .count()

            if (tokenCount >= maxTokens) {
                val rowsToDelete =
                    Tokens
                        .select(Tokens.tokenValidation)
                        .orderBy(Tokens.lastUse)
                        .limit((tokenCount - maxTokens + 1).toInt())

                Tokens.deleteWhere { Tokens.tokenValidation inSubQuery rowsToDelete }
            }

            val inserts =
                Tokens
                    .insert {
                        it[Tokens.username] = token.username
                        it[Tokens.tokenValidation] = token.tokenValidationInfo
                        it[Tokens.lastUse] = token.lastUsedAt.epochSeconds
                    }.insertedCount

            return@transaction if (inserts < 1) {
                failure(UserError.UnexpectedError)
            } else {
                success(Unit)
            }
        }

    override fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ): Either<UserError, Unit> =
        transaction {
            val lastUsage =
                Tokens
                    .select(Tokens.lastUse)
                    .where(Tokens.tokenValidation eq token.tokenValidationInfo)
                    .firstOrNull() ?: return@transaction failure(UserError.TokenDoesNotExist)

            if (now.epochSeconds - lastUsage[Tokens.lastUse] > tokenLifetime.inWholeSeconds) {
                Tokens.deleteWhere { Tokens.tokenValidation eq token.tokenValidationInfo }
                return@transaction failure(UserError.ExpiredToken)
            }

            val count =
                Tokens.update({ Tokens.tokenValidation eq token.tokenValidationInfo }) {
                    it[Tokens.lastUse] = now.epochSeconds
                }

            return@transaction if (count < 1) {
                failure(UserError.TokenDoesNotExist)
            } else {
                success(Unit)
            }
        }

    override fun removeTokenByValidationInfo(tokenValidationInfo: String): Int =
        Tokens.deleteWhere { tokenValidation eq tokenValidationInfo }
}
