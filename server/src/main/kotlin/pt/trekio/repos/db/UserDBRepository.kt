package pt.trekio.repos.db

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.deleteReturning
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
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
import pt.trekio.repos.db.exposed.Tokens
import pt.trekio.repos.db.exposed.Users
import pt.trekio.security.PasswordEncoder
import kotlin.time.Clock
import kotlin.time.Instant

class UserDBRepository(
    conn: String,
    user: String,
    password: String,
) : UserRepository() {
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
    }

    init {
        transaction(Database.connect(conn, DRIVER_NAME, user, password)) {
            val batch = mutableListOf<String>()
            if (!Users.exists()) {
                batch.addAll(Users.ddl)
            }
            if (!Tokens.exists()) {
                batch.addAll(Tokens.ddl)
            }
            if (batch.isNotEmpty()) {
                batch.forEach(this::exec)
            }

            try {
                exec("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users(lower(username))")
                exec("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(lower(email))")
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun createUser(
        name: Username,
        email: Email,
        password: Password?,
    ): Either<DomainError, User> =
        suspendTransaction {
            if (Users.select(Users.username).any { it[Users.username] == name.value }) {
                return@suspendTransaction failure(UserError.UsernameAlreadyExists)
            }

            if (Users.select(Users.email).any { it[Users.email] == email.value }) {
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
            .map { it.toUser() }
    }

    override suspend fun updateUser(
        name: Username,
        updatedInfo: User,
    ): Either<UserError, Unit> =
        suspendTransaction {
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
            val changes = Users.deleteReturning(listOf(Users.id)) { Users.username eq username.value }

            if (changes.any()) {
                changes.forEach {
                    Tokens.deleteWhere { Tokens.uid eq it[Tokens.uid] }
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
}
