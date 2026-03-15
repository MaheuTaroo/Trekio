package pt.trekio

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
    }
}

//private val logger = LoggerFactory.getLogger("PadelHub")
//
//fun main() {
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
//}
//
///**
// * Builds the application routes using the provided web API.
// *
// * @param webApi the web API instance.
// * @return the application routes.
// */
//fun buildAppRoutes(webApi: WebApi): HttpHandler {
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
//}
