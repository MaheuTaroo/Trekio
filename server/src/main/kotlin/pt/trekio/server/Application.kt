package pt.trekio.server

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import pt.trekio.SERVER_PORT
import pt.trekio.api.TrailApi
import pt.trekio.api.UserApi
import pt.trekio.repos.contracts.TrailRepository
import pt.trekio.repos.contracts.UserRepository
import pt.trekio.repos.db.TrailDBRepository
import pt.trekio.repos.db.UserDBRepository
import pt.trekio.repos.mem.TrailMemoryRepository
import pt.trekio.repos.mem.UserMemoryRepository
import pt.trekio.services.TrailService
import pt.trekio.services.UserService
import java.io.PrintStream

fun printAllowedFlags(stream: PrintStream = System.out) {
    stream.println("Usage:")
    stream.println("\t-mem: uses in-memory repositories")
    stream.println("\t-db: uses database repositories")
    stream.println("\t\t- Syntax: -db <PostgreSQL database URL> <username> <password>")
    stream.println("\t\t- Possible database login combinations:")
    stream.println("\t\t\t - Nothing (environment variables will be used)")
    stream.println("\t\t\t - URL (user and password will not be pulled from environment variables)")
    stream.println("\t\t\t - URL + user (password will not be pulled from environment variables")
    stream.println("\t\t\t - URL + user + password")
    stream.println(System.lineSeparator() + "Excess arguments on either flag will be ignored ")
}

fun Application.configureTrekio(
    userRepo: UserRepository,
    trailRepo: TrailRepository,
) {
    val bearerScheme = "trekio-bearer"

    val userServ = UserService(userRepo)
    val userApi = UserApi(userServ)
    val trailApi = TrailApi(TrailService(trailRepo, userRepo))

    installContentNegotiation()
    installSecuritySchemes(userServ, bearerScheme)

    routing {
        configureOpenAPI()

        configureUserRoutes(userApi, bearerScheme)
        configureTrailRoutes(trailApi, bearerScheme)
    }
}

fun configureDatabaseRepositories(args: List<String>): Pair<UserRepository, TrailRepository> {
    var url: String
    var user: String
    var pass: String

    when (args.size) {
        0 -> {
            val tmp = System.getenv("DB_URL")
            require(tmp != null) { "Missing database URL" }
            url = tmp
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
            pass = args [2]
        }
    }

    return UserDBRepository(url, user, pass) to TrailDBRepository(url, user, pass)
}

fun startServerWith(
    userRepo: UserRepository,
    trailRepo: TrailRepository,
) {
    embeddedServer(
        Netty,
        SERVER_PORT,
        module = { configureTrekio(userRepo, trailRepo) },
    ).start(wait = true)
}

fun main(args: Array<String>) {
    when {
        args.isEmpty() ->
            printAllowedFlags()

        args[0] == "-mem" -> {
            println("Using in-memory repositories")
            startServerWith(UserMemoryRepository, TrailMemoryRepository)
        }


        args[0] == "-db" -> {
            println("Configuring server for database repositories...")

            var userRepo: UserRepository
            var trailRepo: TrailRepository
            try {
                val repos = configureDatabaseRepositories(args.drop(1))
                userRepo = repos.first
                trailRepo = repos.second
            } catch (e: Exception) {
                val text = e.message ?: "An error occurred initializing the database repositories"
                System.err.println(text + System.lineSeparator())
                printAllowedFlags(System.err)
                return
            }

            println("Database repositories configured")
            startServerWith(userRepo, trailRepo)
        }

        else ->
            printAllowedFlags()
    }
}

// private val logger = LoggerFactory.getLogger("PadelHub")
//
// fun main() {
//    val webApi =
//        WebApi(
//            DataBaseMemory(
//                System.getenv("PADELHUB_DB")
//                    ?: "jdbc:postgresql://localhost/postgres?user=postgres&password=postgres",
//            ),
//        )
//    val app = buildAppRoutes(webApi)
//
//    val jettyServer = app.asServer(Jetty(8080)).start()
//    logger.info("server has started")
//
//    readln()
//    jettyServer.stop()
//
//    logger.info("server has stopped")
// }
//
// /**
// * Builds the application routes using the provided web API.
// *
// * @param webApi the web API instance.
// * @return the application routes.
// */
// fun buildAppRoutes(webApi: WebApi): HttpHandler {
//    val usersRoutes =
//        routes(
//            "" bind POST to webApi::createUser,
//            "/{id}" bind GET to webApi::getDetailsUser,
//            "/{name}/{password}" bind GET to webApi::getUserByNameAndPassword,
//            "/match/{id}/{token}" bind GET to webApi::checkIDTokenMatch,
//        )
//    val clubsRoutes =
//        routes(
//            "" bind POST to webApi::createClub,
//            "" bind GET to webApi::getClubs,
//            "/{cid}" bind GET to webApi::getDetailsClub,
//        )
//    val courtsRoutes =
//        routes(
//            "" bind POST to webApi::createCourt,
//            "" bind GET to webApi::getCourts,
//            "/{crid}" bind GET to webApi::getDetailsCourt,
//        )
//    val rentalsRoutes =
//        routes(
//            "/mine" bind GET to webApi::getUserRentals,
//            "/{rid}" bind DELETE to webApi::deleteRental,
//            "/{rid}" bind PUT to webApi::updateRental,
//            "/{rid}" bind GET to webApi::getDetailsRental,
//            "/{cid}/{crid}" bind POST to webApi::createRental,
//            "/{crid}/all" bind GET to webApi::getCourtRentals,
//            "/availability/{crid}/{date}" bind GET to webApi::getAvailableHours,
//        )
//
//    return webApi.functionCaller(
//        routes(
//            "users" bind usersRoutes,
//            "clubs" bind clubsRoutes,
//            "clubs/{cid}/courts" bind courtsRoutes,
//            "rentals" bind rentalsRoutes,
//            singlePageApp(ResourceLoader.Directory("webpage")),
//        ),
//    )
// }
