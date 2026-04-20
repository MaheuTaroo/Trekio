package pt.trekio.server.config

import io.ktor.http.HttpStatusCode
import io.ktor.openapi.OpenApiInfo
import io.ktor.serialization.ContentConvertException
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.origin
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routingRoot
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pt.trekio.api.TrailApi
import pt.trekio.api.UserApi
import pt.trekio.dto.ErrorMessage
import pt.trekio.misc.Success
import pt.trekio.server.config.RouteDescriptions.Users.describeLogin
import pt.trekio.server.config.RouteDescriptions.Users.describeLogout
import pt.trekio.server.config.RouteDescriptions.Users.describeUserByName
import pt.trekio.server.config.RouteDescriptions.Users.describeUserCreation
import pt.trekio.server.config.RouteDescriptions.Users.describeUserDeletion
import pt.trekio.server.config.RouteDescriptions.Users.describeUserInfo
import pt.trekio.server.config.RouteDescriptions.Users.describeUserList
import pt.trekio.services.UserService

fun Application.installContentNegotiation() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }
}

fun Application.installSecuritySchemes(
    userServ: UserService,
    bearerScheme: String,
) {
    install(Authentication) {
        bearer(bearerScheme) {
            authenticate {
                val res = userServ.getOwnDetails(it.token)
                if (res is Success) {
                    it.token to res.value.username
                } else {
                    null
                }
            }
        }
    }
}

fun Route.configureOpenAPI() {
    openAPI(path = "docs") {
        info =
            OpenApiInfo(
                "Trekio API",
                "1.0",
                "OpenAPI documentation for the Trekio API service",
            )
        source =
            OpenApiDocSource.Routing {
                routingRoot.descendants()
            }
    }
}

fun Route.configureUserRoutes(
    userApi: UserApi,
    vararg authSchemes: String,
) {
    route("users") {
        post("create", userApi.createUser()).describeUserCreation()
        post("login", userApi.logUserIn()).describeLogin()

        authenticate(*authSchemes) {
            get(userApi.getUsers()).describeUserList()
            get("self", userApi.getSelf()).describeUserInfo()
            get("{username}", userApi.getUserByName()).describeUserByName()
            delete("delete", userApi.removeUser()).describeUserDeletion()
            delete("logout", userApi.logUserOut()).describeLogout()
        }

        /*put("update/{name}") {
            val name = call.pathParameters["name"]
        }*/
    }
}

fun Route.configureTrailRoutes(
    trailApi: TrailApi,
    vararg authSchemes: String,
) {
    route("trails") {
        authenticate(*authSchemes) {
            post("create", trailApi.createTrail())
            get("available", trailApi.getAvailableTrails())
            get("{tid}", trailApi.getTrail())
            put("{tid}", trailApi.updateTrail())
            delete("{tid}", trailApi.removeTrail())
        }
    }

    route("users") {
        authenticate(*authSchemes) {
            get("{uid}/trails", trailApi.getTrailsOfUser())
        }
    }
}

fun Application.installMalformedBodyCatcher() {
    install(
        createApplicationPlugin("MalformedBodyCatcher") {
            val possibleExceptions = arrayOf(SerializationException::class, ContentConvertException::class)
            on(io.ktor.server.application.hooks.CallFailed) { call, ex ->
                var currEx: Throwable? = ex

                while (currEx != null) {
                    if (possibleExceptions.any { it.isInstance(currEx) }) {
                        val client = "${call.request.origin.remoteAddress}:${call.request.origin.remotePort}"
                        println("Detected body malformation from $client while accessing ${call.request.uri}")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorMessage(
                                "Incorrect body formation detected; check /docs for the full Trekio API route documentation",
                            ),
                        )
                        break
                    } else {
                        currEx = currEx.cause
                    }
                }
            }
        },
    )
}
