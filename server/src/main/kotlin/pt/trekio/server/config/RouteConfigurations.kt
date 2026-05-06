package pt.trekio.server.config

import com.auth0.jwt.JWT
import io.ktor.client.HttpClient
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
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.oauth
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
import pt.trekio.errors.UserError
import pt.trekio.errors.toErrorMessage
import pt.trekio.misc.Routes.AVAILABLE
import pt.trekio.misc.Routes.CALLBACK
import pt.trekio.misc.Routes.CREATE
import pt.trekio.misc.Routes.DELETE
import pt.trekio.misc.Routes.DOCS
import pt.trekio.misc.Routes.GET_STATS
import pt.trekio.misc.Routes.GET_TRAILS
import pt.trekio.misc.Routes.GOOGLE
import pt.trekio.misc.Routes.HIKES
import pt.trekio.misc.Routes.HIKE_ID
import pt.trekio.misc.Routes.IMPORT
import pt.trekio.misc.Routes.LOGIN
import pt.trekio.misc.Routes.LOGOUT
import pt.trekio.misc.Routes.OAUTH
import pt.trekio.misc.Routes.REFRESH
import pt.trekio.misc.Routes.SELF
import pt.trekio.misc.Routes.TRAILS
import pt.trekio.misc.Routes.TRAILS_IMPORT
import pt.trekio.misc.Routes.TRAIL_ID
import pt.trekio.misc.Routes.TRAIL_START
import pt.trekio.misc.Routes.USERNAME
import pt.trekio.misc.Routes.USERS
import pt.trekio.misc.Routes.USERS_REFRESH
import pt.trekio.misc.Success
import pt.trekio.security.Sha256TokenEncoder.createValidationInformation
import pt.trekio.security.Token
import pt.trekio.server.config.RouteDescriptions.Trails.describeAvailableTrails
import pt.trekio.server.config.RouteDescriptions.Trails.describeSpecificTrail
import pt.trekio.server.config.RouteDescriptions.Trails.describeTrailCreation
import pt.trekio.server.config.RouteDescriptions.Trails.describeTrailDeletion
import pt.trekio.server.config.RouteDescriptions.Trails.describeTrailImport
import pt.trekio.server.config.RouteDescriptions.Trails.describeTrailUpdate
import pt.trekio.server.config.RouteDescriptions.Trails.describeUserTrails
import pt.trekio.server.config.RouteDescriptions.Users.describeLogin
import pt.trekio.server.config.RouteDescriptions.Users.describeLogout
import pt.trekio.server.config.RouteDescriptions.Users.describeRefreshToken
import pt.trekio.server.config.RouteDescriptions.Users.describeUserByName
import pt.trekio.server.config.RouteDescriptions.Users.describeUserCreation
import pt.trekio.server.config.RouteDescriptions.Users.describeUserDeletion
import pt.trekio.server.config.RouteDescriptions.Users.describeUserInfo
import pt.trekio.server.config.RouteDescriptions.Users.describeUserList
import pt.trekio.services.UserService

suspend fun ApplicationCall.sendError(err: DomainError) {
    respond(HttpStatusCode.fromValue(err.statusCode), err.toErrorMessage())
}

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
    jwtScheme: String,
    oauthScheme: String,
    client: HttpClient,
) {
    install(Authentication) {
        configureOAuth(oauthScheme, client)

        configureJwt(jwtScheme, userServ)

        configureBearer(bearerScheme, userServ)
    }
}

fun AuthenticationConfig.configureOAuth(
    oauthScheme: String,
    httpClient: HttpClient,
) {
    oauth(oauthScheme) {
        urlProvider = { "http://localhost:8080/users/oauth/callback" }
        providerLookup = {
            OAuthServerSettings.OAuth2ServerSettings(
                name = "google",
                authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                requestMethod = HttpMethod.Post,
                clientId = System.getenv("GOOGLE_CLIENT_ID"),
                clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
                defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile"),
                extraAuthParameters = listOf("access_type" to "offline"),
            )
        }
        client = httpClient
    }
}

fun AuthenticationConfig.configureJwt(
    jwtScheme: String,
    userServ: UserService,
) {
    jwt(jwtScheme) {
        val jwtVerifier =
            JWT
                .require(Token.algorithm)
                .build()

        verifier(jwtVerifier)
        validate { credential ->
            val username = credential.payload.getClaim("username").asString()
            if (username.isNullOrBlank()) {
                return@validate null
            }
            val user = userServ.getUser(username)
            if (user is Success) {
                user.value.id
            } else {
                null
            }
        }
        challenge { _, _ ->
            call.sendError(UserError.ExpiredToken)
        }
    }
}

fun AuthenticationConfig.configureBearer(
    bearerScheme: String,
    userServ: UserService,
) {
    bearer(bearerScheme) {
        authenticate {
            val validationToken = createValidationInformation(it.token)
            val res = userServ.getUserByToken(validationToken)
            if (res is Success) {
                validationToken to res.value.id
            } else {
                null
            }
        }
    }
}

fun Route.configureOpenAPI() {
    openAPI(path = DOCS) {
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
    oauthScheme: String,
    jwtScheme: String,
    bearerScheme: String,
) {
    route(USERS) {
        post(CREATE, userApi.createUser()).describeUserCreation()
        post(LOGIN, userApi.logUserIn()).describeLogin()

        route(OAUTH) {
            authenticate(oauthScheme) {
                get(GOOGLE) {}
                get(CALLBACK, userApi.createUser()).describeUserCreation()
            }
        }

        authenticate(jwtScheme) {
            get(userApi.getUsers()).describeUserList()
            get(SELF, userApi.getSelf()).describeUserInfo()
            get(USERNAME, userApi.getUserByName()).describeUserByName()
        }

        authenticate(bearerScheme) {
            put(REFRESH, userApi.refreshToken()).describeRefreshToken()
            delete(LOGOUT, userApi.logUserOut()).describeLogout()
            delete(DELETE, userApi.removeUser()).describeUserDeletion()
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
    route(TRAILS) {
        authenticate(*authSchemes) {
            post(CREATE, trailApi.createTrail()).describeTrailCreation()

            val importRoute: Route.() -> Unit = {
                post(IMPORT, trailApi.importTrail()).describeTrailImport()
            }
            contentType(ContentType.Application.Xml, importRoute)
            contentType(ContentType.Text.Xml, importRoute)
            contentType(
                ContentType("application", "vnd.google-earth.kml+xml"),
                importRoute,
            )

            get(AVAILABLE, trailApi.getAvailableTrails()).describeAvailableTrails()
            get(TRAIL_ID, trailApi.getTrail()).describeSpecificTrail()
            put(TRAIL_ID, trailApi.updateTrail()).describeTrailUpdate()
            delete(TRAIL_ID, trailApi.removeTrail()).describeTrailDeletion()
        }
    }

    route(USERS) {
        authenticate(*authSchemes) {
            get(GET_TRAILS, trailApi.getTrailsOfUser()).describeUserTrails()
        }
    }
}

fun Route.configureHikeRoutes(
    hikeApi: HikeApi,
    vararg authSchemes: String,
) {
    route(TRAILS) {
        authenticate(*authSchemes) {
            post(TRAIL_START, hikeApi.startHike())
        }
    }

    route(HIKES) {
        authenticate(*authSchemes) {
            get(HIKE_ID, hikeApi.getDetails())
            put(TRAIL_ID, hikeApi.finishHike())
            delete(TRAIL_ID, hikeApi.cancelHike())
        }
    }

    route(USERS) {
        authenticate(*authSchemes) {
            get(GET_STATS, hikeApi.getStats()).describeUserTrails()
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
                if (call.request.httpMethod !in bodyMethods || call.request.path() == USERS_REFRESH) return@intercept

                val allowedTypes = if (call.request.path() == TRAILS_IMPORT) trailImportTypes else defaultTypes
                val contentType = call.request.contentType()

                if (contentType !in allowedTypes) {
                    call.sendError(
                        DomainError
                            .IncorrectMediaType(allowedTypes.map { "${it.contentType}/${it.contentSubtype}" }),
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
