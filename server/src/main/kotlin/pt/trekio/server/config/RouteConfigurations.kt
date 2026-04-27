package pt.trekio.server.config

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.OpenApiInfo
import io.ktor.serialization.ContentConvertException
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.request.contentType
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.contentType
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routingRoot
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pt.trekio.api.HikeApi
import pt.trekio.api.TrailApi
import pt.trekio.api.UserApi
import pt.trekio.dto.ErrorMessage
import pt.trekio.errors.DomainError
import pt.trekio.errors.toErrorMessage
import pt.trekio.misc.Success
import pt.trekio.server.config.RouteDescriptions.Trails.describeAvailableTrails
import pt.trekio.server.config.RouteDescriptions.Trails.describeSpecificTrail
import pt.trekio.server.config.RouteDescriptions.Trails.describeTrailCreation
import pt.trekio.server.config.RouteDescriptions.Trails.describeTrailDeletion
import pt.trekio.server.config.RouteDescriptions.Trails.describeTrailImport
import pt.trekio.server.config.RouteDescriptions.Trails.describeTrailUpdate
import pt.trekio.server.config.RouteDescriptions.Trails.describeUserTrails
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
            post("create", trailApi.createTrail()).describeTrailCreation()

            val importRoute: Route.() -> Unit = {
                post("import", trailApi.importTrail()).describeTrailImport()
            }
            contentType(ContentType.Application.Xml, importRoute)
            contentType(ContentType.Text.Xml, importRoute)
            contentType(
                ContentType("application", "vnd.google-earth.kml+xml"),
                importRoute,
            )

            get("available", trailApi.getAvailableTrails()).describeAvailableTrails()
            get("{tid}", trailApi.getTrail()).describeSpecificTrail()
            put("{tid}", trailApi.updateTrail()).describeTrailUpdate()
            delete("{tid}", trailApi.removeTrail()).describeTrailDeletion()
        }
    }

    route("users") {
        authenticate(*authSchemes) {
            get("{uid}/trails", trailApi.getTrailsOfUser()).describeUserTrails()
        }
    }
}

fun Route.configureHikeRoutes(
    hikeApi: HikeApi,
    vararg authSchemes: String,
) {
    route("trails") {
        authenticate(*authSchemes) {
            post("{tid}/start", hikeApi.startHike())
        }
    }

    route("hikes") {
        authenticate(*authSchemes) {
            get("{hid}", hikeApi.getDetails())
            put("{tid}", hikeApi.finishHike())
            delete("{tid}", hikeApi.cancelHike())
        }
    }

    route("users") {
        authenticate(*authSchemes) {
            get("{username}/trails", hikeApi.getStats()).describeUserTrails()
        }
    }
}

fun Application.installRequestBodyWatchdog() {
    val trailImportTypes =
        setOf(
            ContentType.Application.Xml,
            ContentType.Text.Xml,
            ContentType("application", "vnd.google-earth.kml+xml"),
        )

    val defaultTypes = setOf(ContentType.Application.Json)

    val bodyMethods = setOf(HttpMethod.Post, HttpMethod.Put)

    install(
        createApplicationPlugin("RequestBodyWatchdog") {
            application.intercept(ApplicationCallPipeline.Plugins) {
                if (call.request.httpMethod !in bodyMethods) return@intercept

                val allowedTypes = if (call.request.path() == "/trails/import") trailImportTypes else defaultTypes
                val contentType = call.request.contentType()

                if (contentType !in allowedTypes) {
                    call.respond(
                        HttpStatusCode.UnsupportedMediaType,
                        DomainError
                            .IncorrectMediaType(
                                allowedTypes.map { "${it.contentType}/${it.contentSubtype}" },
                            ).toErrorMessage(),
                    )
                    return@intercept
                }

                runCatching { proceed() }
                    .onFailure { t -> call.handleBodyError(t) }
            }
        },
    )
}

private suspend fun ApplicationCall.handleBodyError(t: Throwable) {
    if (!t.isMalformedBody()) throw t
    respond(
        HttpStatusCode.UnsupportedMediaType,
        ErrorMessage("Incorrect body formation detected; check /docs for the full Trekio API route documentation"),
    )
}

private fun Throwable.isMalformedBody(): Boolean =
    generateSequence(this) { it.cause }.any { cause ->
        cause is SerializationException ||
            cause is ContentConvertException ||
            cause is ContentTransformationException // from call.receive
    }
