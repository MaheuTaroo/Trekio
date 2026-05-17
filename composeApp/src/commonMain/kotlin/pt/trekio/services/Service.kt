package pt.trekio.services

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.http.path
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.UserRepo
import kotlin.time.Clock

abstract class Service(
    protected val userRepo: UserRepo,
    protected val webClient: HttpClient,
) {
    companion object {
        @PublishedApi
        internal val refreshMutex = Mutex()
    }

    protected suspend inline fun <reified T> generateJsonResponse(
        noinline requestGenerator: suspend (String, String) -> HttpResponse,
        route: ApiRoutes,
        noinline onSuccess: suspend (T) -> Unit,
    ): Either<String, T> {
        val sessionCheck = checkLocalSessionValidity(route)
        if (sessionCheck is Failure) return sessionCheck

        try {
            val requestResult = executeWithAutoRefresh(route, requestGenerator)
            if (requestResult is Failure) return requestResult

            val res = (requestResult as Success).value

            val possibleErr = onBadResponse(res)
            if (possibleErr is Failure) return possibleErr

            val body = res.body<T>()
            onSuccess(body)
            return success(body)
        } catch (t: Throwable) {
            Logger.e("generateJsonResponse") { t.message ?: "I frew up :(" }
            return failure("")
        }
    }

    protected suspend fun onBadResponse(res: HttpResponse): Either<String, Unit> {
        if (res.status >= HttpStatusCode.BadRequest) {
            val err = res.body<ErrorMessage>().error
            if (res.status == HttpStatusCode.Unauthorized || res.status == HttpStatusCode.Forbidden) {
                userRepo.clear()
                return failure(err)
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
            Logger.e("body is not ErrorMessage") { e.message.toString() }
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
                Logger.i("New access token: ${newTokens.accessTokenValue}")
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
            Logger.e("Token refresh failed") { e.message.toString() }
            failure(e.message ?: "Connection error while refreshing token")
        }
    }

    @PublishedApi
    internal suspend fun executeWithAutoRefresh(
        route: ApiRoutes,
        requestGenerator: suspend (String, String) -> HttpResponse,
    ): Either<String, HttpResponse> {
        refreshMutex.withLock { }
        var currentToken = getAuthToken(route.requireAuthType)
        if ((route.requireAuthType == AuthType.JWT || route.requireAuthType == AuthType.BEARER) &&
            currentToken.isEmpty()
        ) {
            return failure("Session expired")
        }

        var res = requestGenerator(currentToken, route.path)

        if (res.status == HttpStatusCode.Forbidden && isExpiredTokenError(res)) {
            refreshMutex.withLock {
                val tokenAfterLock = getAuthToken(route.requireAuthType)

                if (currentToken == tokenAfterLock) {
                    Logger.i("Refreshing token...")
                    val refreshResult = performTokenRefresh()
                    if (refreshResult is Failure) {
                        userRepo.clear()
                        return refreshResult
                    }
                    Logger.i("Token successfully refreshed!, continuing...")
                } else {
                    Logger.i("Token has been refreshed by another request, continuing...")
                }
            }

            currentToken = getAuthToken(route.requireAuthType)
            Logger.i("Retrying request!")
            res = requestGenerator(currentToken, route.path)
        }

        return success(res)
    }
}
