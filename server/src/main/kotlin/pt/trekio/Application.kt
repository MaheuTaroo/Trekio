package pt.trekio

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import pt.trekio.domain.User
import pt.trekio.dto.ErrorMessage
import pt.trekio.dto.UserCreate
import pt.trekio.dto.UserList
import pt.trekio.errors.UserError
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.repos.mem.UserMemoryRepository
import pt.trekio.services.UserService

fun Route.userRoutes(service: UserService) {
    route("users") {
        get {
            val skip = call.queryParameters["skip"]?.toIntOrNull() ?: 0
            val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 10

            val res = service.getUsers(skip, limit)
            if (res is Failure) {
                call.respond(HttpStatusCode.BadRequest, ErrorMessage(res.message.error))
            } else {
                call.respond(UserList((res as Success).value.map(User::toUserDto)))
            }
        }

        get("self") {
            val token = call.request.header("Authorization")
            println("Authorization: $token")
            if (token == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorMessage(UserError.MissingToken.error))
                return@get
            }

            val res = service.getOwnDetails(token)
            if (res is Failure) {
                call.respond(HttpStatusCode.BadRequest, ErrorMessage(res.message.error))
            } else {
                call.respond((res as Success).value.toUserDto())
            }
        }

        get("{name}") {
            val name = call.pathParameters["name"] as String
            val res = service.getUser(name)
            if (res is Failure) {
                call.respond(HttpStatusCode.BadRequest, ErrorMessage(res.message.error))
            } else {
                call.respond((res as Success).value.toUserDto())
            }
        }

        post("create") {
            try {
                val user = call.receive<UserCreate>()
                val res = service.createUser(user.username, user.email, user.password)
                if (res is Failure) {
                    call.respond(HttpStatusCode.BadRequest, ErrorMessage(res.message.error))
                } else {
                    call.respond((res as Success).value.toTokenExternalInfo())
                }
            } catch (_: Exception) {
                call.respond(
                    ErrorMessage("Request body is missing or malformed; check /docs or /documentation.html on how to use the Trekio API"),
                )
            }
        }

        /*put("update/{name}") {
            val name = call.pathParameters["name"]
        }*/

        delete("delete") {
            val token = call.request.header("Authorization")
            if (token == null) {
                call.respond(ErrorMessage(UserError.MissingToken.error))
                return@delete
            }

            val res = service.deleteUser(token)
            if (res is Failure) {
                call.respond(HttpStatusCode.BadRequest, ErrorMessage(res.message.error))
            } else {
                call.respondText("User deleted successfully")
            }
        }
    }
}

fun Application.module() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }
    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }

        userRoutes(UserService(UserMemoryRepository))
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
