package pt.trekio

import io.ktor.openapi.OpenApiInfo
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.routing.routingRoot
import kotlinx.serialization.json.Json
import pt.trekio.api.UserApi
import pt.trekio.misc.Success
import pt.trekio.repos.mem.UserMemoryRepository
import pt.trekio.services.UserService

const val AUTH_SCHEME = "trekio-bearer"

fun Route.userRoutes(userApi: UserApi) {
    route("users") {
        post("create", userApi.createUser())
        post("login", userApi.logUserIn())

        authenticate(AUTH_SCHEME) {
            get(userApi.getUsers())
            get("self", userApi.getSelf())
            get("{name}", userApi.getUserByName())
            delete("delete", userApi.removeUser())
            delete("logout", userApi.logUserOut())
        }

        /*put("update/{name}") {
            val name = call.pathParameters["name"]
        }*/
    }
}

fun Application.module() {
    val memLayer = UserMemoryRepository
    val serviceLayer = UserService(memLayer)
    val api = UserApi(serviceLayer)

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }
    install(Authentication) {
        bearer(AUTH_SCHEME) {
            authenticate {
                val res = serviceLayer.getOwnDetails(it.token)
                if (res is Success) {
                    it.token to res.value.username
                } else {
                    null
                }
            }
        }
    }

    routing {
        openAPI(path = "docs") {
            info = OpenApiInfo("Trekio API", "1.0")
            source =
                OpenApiDocSource.Routing {
                    routingRoot.descendants()
                }
        }

        userRoutes(api)
    }
}

fun main() {
    embeddedServer(Netty, SERVER_PORT, module = Application::module)
        .start(wait = true)
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
