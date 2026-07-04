package pt.trekio.services

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.http.path
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pt.trekio.dto.ErrorMessage
import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.errors.UserError
import pt.trekio.errors.toErrorMessage
import pt.trekio.misc.ApiRoutes
import pt.trekio.misc.ApiRoutes.UserRefresh
import pt.trekio.misc.AuthType
import pt.trekio.misc.Either
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.misc.ReusableSuspendableHolder
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.UserRepository
import kotlin.time.Clock

abstract class Service(
    protected val userRepo: UserRepository,
    protected val webClient: HttpClient,
) {
    protected companion object {
        private val refreshMutex = Mutex()

        private val socketClosingReasonHolder = ReusableSuspendableHolder<Deferred<CloseReason?>>()

        @PublishedApi
        internal val logger = Logger.withTag(this::class.simpleName!!)

        @PublishedApi
        internal fun URLBuilder.applyPagination(page: ULong) {
            parameters.apply {
                append("skip", "${page.coerceAtLeast(0uL) * PAGE_SIZE}")
                append("limit", "$PAGE_SIZE")
            }
        }

        protected const val PAGE_SIZE = 10u
    }

    protected suspend inline fun <reified T> checkSession(
        route: ApiRoutes,
        block: () -> Either<String, T>
    ): Either<String, T> {
        val sessionCheck = checkLocalSessionValidity(route)
        if (sessionCheck is Failure) return sessionCheck

        try {
            return block()
        } catch (t: Throwable) {
            logger.e { "handleRequest: ${t.message ?: "I frew up :("}" }
            return failure("An error occurred, please try again later")
        }
    }

    protected suspend inline fun <reified T> generateJsonResponse(
        route: ApiRoutes,
        noinline request: suspend HttpClient.(String, String) -> HttpResponse,
        onSuccess: suspend (T) -> Unit,
    ): Either<String, T> =
        checkSession(route) {
            val requestResult = executeWithAutoRefresh(route, request)
            if (requestResult is Failure) return requestResult

            val res = (requestResult as Success).value

            val possibleErr = onBadResponse(res)
            if (possibleErr is Failure) return possibleErr

            return if (Unit is T) {
                success(Unit)
            } else {
                val body = res.body<T>()
                onSuccess(body)
                success(body)
            }
        }

    protected suspend fun generateWebSocketStream(
        route: ApiRoutes,
        request: HttpRequestBuilder.(String, String) -> Unit,
        onFrame: (Frame.Text) -> Unit
    ): Either<String, Unit> =
        checkSession(route) {
            val currToken = refreshMutex.withLock { getAuthToken(route.requireAuthType) }
            withContext(Dispatchers.Default) {
                webClient.webSocket({ request(route.path, currToken) }) {
                    socketClosingReasonHolder.set(closeReason)
                    incoming.receiveAsFlow().collect {
                        if (it is Frame.Text)
                            onFrame(it)
                    }
                }
            }
            if (socketClosingReasonHolder.get().await()?.knownReason == CloseReason.Codes.CANNOT_ACCEPT) {
                refreshToken(
                    currToken,
                    route.requireAuthType
                )
                webClient.webSocket({ request(route.path, currToken) }) {
                    socketClosingReasonHolder.set(closeReason)
                    incoming.receiveAsFlow().collect {
                        if (it is Frame.Text)
                            onFrame(it)
                    }
                }
            }
            val reason = socketClosingReasonHolder.get().await()
            return if (reason == null || reason.knownReason == null || reason.knownReason == CloseReason.Codes.NORMAL)
                    success(Unit)
                else
                    failure(
                        reason.message.ifBlank { "Server closed socket with error ${reason.knownReason!!.name}" }
                    )
        }

    protected suspend fun onBadResponse(res: HttpResponse): Either<String, Unit> {
        if (res.status >= HttpStatusCode.BadRequest) {
            val err = res.body<ErrorMessage>().error
            if (res.status == HttpStatusCode.Unauthorized || res.status == HttpStatusCode.Forbidden) {
                userRepo.clear()
            }
            return failure(err)
        }
        return success(Unit)
    }

    @PublishedApi
    internal suspend fun checkLocalSessionValidity(route: ApiRoutes): Either<String, Unit> {
        if (route.requireAuthType != AuthType.JWT && route.requireAuthType != AuthType.BEARER) {
            return success(Unit)
        }
        val now = Clock.System.now().toEpochMilliseconds() / 1000
        val tokens = userRepo.getTokens() ?: return failure("You are not logged in")

        if (tokens.expiration < now) {
            userRepo.clear()
            return failure("You got logged out by inactivity")
        }
        return success(Unit)
    }

    @PublishedApi
    internal suspend fun getAuthToken(authType: AuthType): String =
        when (authType) {
            AuthType.JWT -> userRepo.getTokens()?.accessToken ?: ""
            AuthType.BEARER -> userRepo.getTokens()?.refreshToken ?: ""
            else -> ""
        }

    @PublishedApi
    internal suspend fun isExpiredTokenError(res: HttpResponse): Boolean =
        try {
            val err = res.body<ErrorMessage>().error
            err == UserError.ExpiredToken.toErrorMessage().error
        } catch (e: Exception) {
            logger.e("${e.message}")
            false
        }

    @PublishedApi
    internal suspend fun performTokenRefresh(): Either<String, Unit> {
        return try {
            val refreshToken = userRepo.getTokens()?.refreshToken ?: return failure("You got logged out by inactivity")
            val res =
                webClient.put {
                    url.path(UserRefresh.path)
                    headers {
                        bearerAuth(refreshToken)
                    }
                }
            if (res.status.isSuccess()) {
                val newTokens = res.body<TokenExternalInfoDto>()
                logger.i("New access token: ${newTokens.accessTokenValue}")
                userRepo.saveToken(
                    newTokens.accessTokenValue,
                    newTokens.refreshTokenValue,
                    newTokens.tokenExpiration,
                )
                success(Unit)
            } else {
                val err =
                    try {
                        res.body<ErrorMessage>().error
                    } catch (e: Exception) {
                        e.message.toString()
                    }
                failure(err)
            }
        } catch (e: Exception) {
            logger.e(e.message.toString())
            failure(e.message ?: "Connection error while refreshing token")
        }
    }

    @PublishedApi
    internal suspend fun executeWithAutoRefresh(
        route: ApiRoutes,
        request: suspend HttpClient.(String, String) -> HttpResponse,
    ): Either<String, HttpResponse> {
        var currentToken = refreshMutex.withLock { getAuthToken(route.requireAuthType) }
        if ((route.requireAuthType == AuthType.JWT || route.requireAuthType == AuthType.BEARER) &&
            currentToken.isEmpty()
        ) {
            return failure("Session expired")
        }

        var res = webClient.request(route.path, currentToken)

        if (res.status == HttpStatusCode.Forbidden && isExpiredTokenError(res)) {
            refreshToken(currentToken, route.requireAuthType)

            currentToken = getAuthToken(route.requireAuthType)
            logger.i("Retrying request...")
            res = webClient.request(route.path, currentToken)
        }

        return success(res)
    }

    internal suspend fun refreshToken(
        currentToken: String,
        requiredAuthType: AuthType
    ): Either<String, Unit> =
        refreshMutex.withLock {
            val tokenAfterLock = getAuthToken(requiredAuthType)

            if (currentToken == tokenAfterLock) {
                logger.i("Refreshing token...")
                val refreshResult = performTokenRefresh()
                if (refreshResult is Failure) {
                    userRepo.clear()
                }
                else {
                    logger.i("Token successfully refreshed, continuing...")
                }
                return refreshResult
            } else {
                logger.i("Token has been refreshed by another request, continuing...")
                return success(Unit)
            }
        }
}
