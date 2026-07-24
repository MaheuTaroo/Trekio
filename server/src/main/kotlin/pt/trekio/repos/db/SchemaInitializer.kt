package pt.trekio.repos.db

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.exists
import org.jetbrains.exposed.v1.r2dbc.insertIgnore
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import pt.trekio.misc.Email
import pt.trekio.misc.UserRank
import pt.trekio.misc.Username
import pt.trekio.repos.db.exposed.HikeMembers
import pt.trekio.repos.db.exposed.Hikes
import pt.trekio.repos.db.exposed.OAuthCodes
import pt.trekio.repos.db.exposed.Tokens
import pt.trekio.repos.db.exposed.Trails
import pt.trekio.repos.db.exposed.Users
import pt.trekio.security.PasswordEncoder

object SchemaInitializer {
    private const val SCHEMA_INIT_LOCK_ID = 918273645L
    private val mutex = Mutex()
    val superUserName = Username("SuperUser")
    val superUserEmail = Email("superuser@gmail.com")
    val superUserPassword =
        PasswordEncoder.encode(
            requireNotNull(System.getenv("TREKIO_SUPERUSER_PASSWORD")) { "Missing TREKIO_SUPERUSER_PASSWORD" },
        )
    val superUserRank = UserRank.VERIFIED
    private var initialized = false

    suspend fun ensureInitialized() {
        mutex.withLock {
            if (initialized) return@withLock

            suspendTransaction {
                suspend fun createIfMissing(table: Table) {
                    if (!table.exists()) {
                        table.ddl.forEach { ddlStatement ->
                            try {
                                exec(ddlStatement)
                            } catch (e: Exception) {
                                println("Falhou ao criar tabela ${table.tableName}: $ddlStatement")
                                e.printStackTrace()
                                throw e
                            }
                        }
                    }
                }

                suspend fun createIndexSafely(sql: String) {
                    exec("SAVEPOINT idx_sp")
                    try {
                        exec(sql)
                        exec("RELEASE SAVEPOINT idx_sp")
                    } catch (_: Exception) {
                        exec("ROLLBACK TO SAVEPOINT idx_sp")
                    }
                }

                exec("SELECT pg_advisory_lock($SCHEMA_INIT_LOCK_ID)")
                try {
                    createIfMissing(Users)

                    createIndexSafely("ALTER TABLE users ADD CONSTRAINT users_email_key UNIQUE (email)")
                    createIndexSafely("ALTER TABLE users ADD CONSTRAINT users_username_key UNIQUE (username)")

                    createIfMissing(Trails)
                    createIfMissing(Hikes)
                    createIfMissing(HikeMembers)
                    createIfMissing(Tokens)
                    createIfMissing(OAuthCodes)

                    Users.insertIgnore {
                        it[username] = superUserName.value
                        it[email] = superUserEmail.value
                        it[passwordValidation] = superUserPassword
                        it[rank] = superUserRank
                    }

                    createIndexSafely("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users(username)")
                    createIndexSafely("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(lower(email))")
                    createIndexSafely("CREATE UNIQUE INDEX IF NOT EXISTS idx_oauth_code ON oauth_codes(code)")
                    createIndexSafely("CREATE UNIQUE INDEX IF NOT EXISTS idx_trails_name ON trails(lower(trail_name))")
                    createIndexSafely("CREATE UNIQUE INDEX IF NOT EXISTS idx_hike_members_hiker_id ON hike_members(hiker_id)")
                } finally {
                    exec("SELECT pg_advisory_unlock($SCHEMA_INIT_LOCK_ID)")
                }
            }

            initialized = true
        }
    }
}
