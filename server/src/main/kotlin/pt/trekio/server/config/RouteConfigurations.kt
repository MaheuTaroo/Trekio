package pt.trekio.server.config

import com.auth0.jwt.JWT
import io.ktor.client.HttpClient
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.OpenApiInfo
import io.ktor.serialization.ContentConvertException
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
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
import io.ktor.server.routing.routingRoot
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pt.trekio.api.HikeApi
import pt.trekio.api.TrailApi
import pt.trekio.api.UserApi
import pt.trekio.dto.ErrorMessage
import pt.trekio.errors.DomainError
import pt.trekio.errors.UserError
import pt.trekio.errors.toErrorMessage
import pt.trekio.misc.ApiRoutes.Docs
import pt.trekio.misc.ApiRoutes.HikeById
import pt.trekio.misc.ApiRoutes.HikeCancelTrail
import pt.trekio.misc.ApiRoutes.HikeFinishByTrailId
import pt.trekio.misc.ApiRoutes.HikeUserStats
import pt.trekio.misc.ApiRoutes.TrailById
import pt.trekio.misc.ApiRoutes.TrailCreate
import pt.trekio.misc.ApiRoutes.TrailDelete
import pt.trekio.misc.ApiRoutes.TrailStart
import pt.trekio.misc.ApiRoutes.TrailUpdate
import pt.trekio.misc.ApiRoutes.TrailsAvailable
import pt.trekio.misc.ApiRoutes.TrailsImport
import pt.trekio.misc.ApiRoutes.UserByUsername
import pt.trekio.misc.ApiRoutes.UserCreate
import pt.trekio.misc.ApiRoutes.UserDelete
import pt.trekio.misc.ApiRoutes.UserLogin
import pt.trekio.misc.ApiRoutes.UserLogout
import pt.trekio.misc.ApiRoutes.UserOauthCallback
import pt.trekio.misc.ApiRoutes.UserOauthLogin
import pt.trekio.misc.ApiRoutes.UserRefresh
import pt.trekio.misc.ApiRoutes.UserSelf
import pt.trekio.misc.ApiRoutes.UserTrails
import pt.trekio.misc.ApiRoutes.Users
import pt.trekio.misc.Routes.BASE_URL
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
import pt.trekio.server.config.RouteDescriptions.Users.describeOauth
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

suspend fun WebSocketServerSession.sendError(err: DomainError) {
    sendSerialized(err.toErrorMessage())
    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT.code, err.message))
}

fun Application.installContentNegotiation() {
    val prettyButLaxJson = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    install(ContentNegotiation) {
        json(prettyButLaxJson)
    }

    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(prettyButLaxJson)
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
        urlProvider = { "https://takisha-unsustaining-unceasingly.ngrok-free.dev/users/oauth/callback" }
        providerLookup = {
            OAuthServerSettings.OAuth2ServerSettings(
                name = "google",
                authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                requestMethod = HttpMethod.Post,
                clientId = System.getenv("GOOGLE_CLIENT_ID"),
                clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
                defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.email"),
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
                .withIssuer(BASE_URL)
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
    openAPI(path = Docs.path) {
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
    client: HttpClient,
) {
    post(UserCreate.path, userApi.createUser()).describeUserCreation()
    post(UserLogin.path, userApi.logUserIn()).describeLogin()

    authenticate(oauthScheme) {
        get(UserOauthLogin.path) {}
        get(UserOauthCallback.path, userApi.oauthAuthentication(client)).describeOauth()
    }

    authenticate(jwtScheme) {
        get(Users.path, userApi.getUsers()).describeUserList()
        get(UserSelf.path, userApi.getSelf()).describeUserInfo()
        get(UserByUsername().path, userApi.getUserByName()).describeUserByName()
    }

    authenticate(bearerScheme) {
        put(UserRefresh.path, userApi.refreshToken()).describeRefreshToken()
        delete(UserLogout.path, userApi.logUserOut()).describeLogout()
        delete(UserDelete.path, userApi.removeUser()).describeUserDeletion()
    }
}

fun Route.configureTrailRoutes(
    trailApi: TrailApi,
    vararg authSchemes: String,
) {
    authenticate(*authSchemes) {
        post(TrailCreate.path, trailApi.createTrail()).describeTrailCreation()

        val importRoute: Route.() -> Unit = {
            post(TrailsImport.path, trailApi.importTrail()).describeTrailImport()
        }
        contentType(ContentType.Application.Xml, importRoute)
        contentType(ContentType.Text.Xml, importRoute)
        contentType(
            ContentType("application", "vnd.google-earth.kml+xml"),
            importRoute,
        )

        get(TrailsAvailable.path, trailApi.getAvailableTrails()).describeAvailableTrails()
        get(TrailById().path, trailApi.getTrail()).describeSpecificTrail()
        put(TrailUpdate().path, trailApi.updateTrail()).describeTrailUpdate()
        delete(TrailDelete().path, trailApi.removeTrail()).describeTrailDeletion()

        get(UserTrails().path, trailApi.getTrailsOfUser()).describeUserTrails()
    }
}

fun Route.configureHikeRoutes(
    hikeApi: HikeApi,
    vararg authSchemes: String,
) {
    authenticate(*authSchemes) {
        post(TrailStart().path, hikeApi.startHike())

        get(HikeById().path, hikeApi.getDetails())
        put(HikeFinishByTrailId().path, hikeApi.finishHike())
        delete(HikeCancelTrail().path, hikeApi.cancelHike())

        get(HikeUserStats().path, hikeApi.getStats()).describeUserTrails()
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
                if (call.request.httpMethod !in bodyMethods || call.request.path() == UserRefresh.path) return@intercept

                val allowedTypes = if (call.request.path() == TrailsImport.path) trailImportTypes else defaultTypes
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
