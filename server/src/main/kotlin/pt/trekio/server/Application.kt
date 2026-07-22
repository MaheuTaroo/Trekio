package pt.trekio.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import pt.trekio.api.HikeApi
import pt.trekio.api.TrailApi
import pt.trekio.api.UserApi
import pt.trekio.redis.RedisService
import pt.trekio.repos.contracts.HikeRepository
import pt.trekio.repos.contracts.TrailRepository
import pt.trekio.repos.contracts.UserRepository
import pt.trekio.repos.db.HikeDBRepository
import pt.trekio.repos.db.SchemaInitializer
import pt.trekio.repos.db.TrailDBRepository
import pt.trekio.repos.db.UserDBRepository
import pt.trekio.repos.mem.HikeMemoryRepository
import pt.trekio.repos.mem.TrailMemoryRepository
import pt.trekio.repos.mem.UserMemoryRepository
import pt.trekio.server.config.configureHikeRoutes
import pt.trekio.server.config.configureOpenAPI
import pt.trekio.server.config.configureTrailRoutes
import pt.trekio.server.config.configureUserRoutes
import pt.trekio.server.config.installContentNegotiation
import pt.trekio.server.config.installRequestBodyWatchdog
import pt.trekio.server.config.installSecuritySchemes
import pt.trekio.services.HikeService
import pt.trekio.services.TrailService
import pt.trekio.services.UserService
import java.io.PrintStream
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

const val OAUTH_SCHEME = "trekio-google-oauth"
const val JWT_SCHEME = "trekio-jwt"
const val BEARER_SCHEME = "trekio-bearer"

fun printAllowedFlags(stream: PrintStream = System.out) {
    stream.println("Usage (excess arguments ignored):")
    stream.println("\t-mem: uses in-memory repositories")
    stream.println("\t-db: uses database repositories")
    stream.println("\t\t- Syntax: -db <PostgreSQL database URL> <username> <password>")
    stream.println("\t\t- Possible database login combinations:")
    stream.println("\t\t\t- Nothing (environment variables will be used)")
    stream.println("\t\t\t- URL (user and password will not be pulled from environment variables)")
    stream.println("\t\t\t- URL + user (password will not be pulled from environment variables")
    stream.println("\t\t\t- URL + user + password")
}

fun Application.configureTrekio(
    userRepo: UserRepository,
    trailRepo: TrailRepository,
    hikeRepo: HikeRepository,
    redisServ: RedisService,
    createDocs: Boolean = true,
) {
    val userServ = UserService(userRepo)

    val client =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                    },
                )
            }
        }

    installContentNegotiation()
    installSecuritySchemes(userServ, BEARER_SCHEME, JWT_SCHEME, OAUTH_SCHEME, client)
    installRequestBodyWatchdog()

    routing {
        if (createDocs) configureOpenAPI()

        val trailServ = TrailService(trailRepo, userRepo)
        configureUserRoutes(
            UserApi(userServ),
            OAUTH_SCHEME,
            JWT_SCHEME,
            BEARER_SCHEME,
            client,
        )
        configureTrailRoutes(TrailApi(trailServ), JWT_SCHEME)
        configureHikeRoutes(
            HikeApi(trailServ, HikeService(hikeRepo, trailRepo, userRepo), redisServ),
            JWT_SCHEME,
        )
    }
}

fun configureDatabaseRepositories(args: List<String>): Triple<UserRepository, TrailRepository, HikeRepository> {
    var url: String
    var user: String
    var pass: String

    when (args.size) {
        0 -> {
            url = requireNotNull(System.getenv("DB_URL")) { "Missing database URL" }
            user = System.getenv("DB_USER") ?: ""
            pass = System.getenv("DB_PASS") ?: ""
        }

        1 -> {
            url = args[0]
            user = ""
            pass = ""
        }

        2 -> {
            url = args[0]
            user = args[1]
            pass = ""
        }

        else -> {
            url = args[0]
            user = args[1]
            pass = args[2]
        }
    }

    val baseOptions =
        ConnectionFactoryOptions
            .parse(url)
            .mutate()
            .option(ConnectionFactoryOptions.USER, user)
            .option(ConnectionFactoryOptions.PASSWORD, pass)
            .build()

    val rawConnectionFactory = ConnectionFactories.get(baseOptions)

    val pool =
        ConnectionPool(
            ConnectionPoolConfiguration
                .builder(rawConnectionFactory)
                .initialSize(10)
                .maxSize(25)
                .maxIdleTime(5.minutes.toJavaDuration())
                .build(),
        )

    R2dbcDatabase.connect(
        connectionFactory = pool,
        databaseConfig =
            R2dbcDatabaseConfig {
                explicitDialect = PostgreSQLDialect()
            },
    )

    runBlocking { SchemaInitializer.ensureInitialized() }

    return Triple(
        UserDBRepository(),
        TrailDBRepository(),
        HikeDBRepository(),
    )
}

fun startServerWith(
    userRepo: UserRepository,
    trailRepo: TrailRepository,
    hikeRepo: HikeRepository,
    redisServ: RedisService,
) {
    embeddedServer(
        Netty,
        8080,
        module = { configureTrekio(userRepo, trailRepo, hikeRepo, redisServ) },
    ).start(wait = true)
}

fun main(args: Array<String>) {
    val redisUri = System.getenv("REDIS_URL")
    if (redisUri == null) {
        System.err.println("Missing environment variable REDIS_URL, quitting...")
        return
    }

    val redisServ: RedisService

    try {
        redisServ = RedisService(redisUri)
    } catch (t: Throwable) {
        System.err.println("Redis client failed to load: ${t.message ?: "an unknown error occurred"}")
        t.printStackTrace()
        return
    }

    when {
        args.isEmpty() ->
            printAllowedFlags()

        args[0] == "-mem" -> {
            println("Using in-memory repositories")
            startServerWith(
                UserMemoryRepository,
                TrailMemoryRepository,
                HikeMemoryRepository,
                redisServ,
            )
        }

        args[0] == "-db" -> {
            println("Configuring server for database repositories...")

            try {
                val (userRepo, trailRepo, hikeRepo) = configureDatabaseRepositories(args.drop(1))

                println("Database repositories configured")
                startServerWith(userRepo, trailRepo, hikeRepo, redisServ)
            } catch (e: Exception) {
                val text = e.message ?: "an unexpected error occurred"
                System.err.println(
                    "An error occurred initializing the database repositories: $text" + System.lineSeparator(),
                )
                printAllowedFlags(System.err)
                return
            }
        }

        else ->
            printAllowedFlags()
    }
}
