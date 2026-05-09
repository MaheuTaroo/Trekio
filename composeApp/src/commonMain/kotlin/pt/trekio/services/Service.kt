package pt.trekio.services

import co.touchlab.kermit.Logger
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import pt.trekio.dto.ErrorMessage
import pt.trekio.misc.Either
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.UserRepo
import kotlin.time.Clock

abstract class Service(
    protected val userRepo: UserRepo,
) {
    companion object {
        protected const val BAD_TOKEN = "Missing or invalid token"
        protected const val SECONDS_IN_A_DAY = 86400
    }

    protected suspend inline fun <reified T> generateJsonResponse(
        requestGenerator: suspend (String) -> HttpResponse,
        isProtectedRoute: Boolean = true,
        shouldRefreshToken: Boolean = true,
        onSuccess: suspend (T) -> Unit,
    ): Either<String, T> {
        val now = Clock.System.now().epochSeconds
        if (isProtectedRoute) {
            val token = userRepo.getToken() ?: return failure("You are not logged in")
            if (token.expiration < now) {
                userRepo.clear()
                return failure("Your token has expired")
            }
        }
        try {
            val res =
                requestGenerator(
                    if (isProtectedRoute) {
                        userRepo.getToken()!!.token
                    } else {
                        ""
                    },
                )
            val possibleErr = onBadResponse(res, isProtectedRoute)
            if (possibleErr is Either.Failure) {
                return possibleErr
            }
            val body = res.body<T>()
            if (isProtectedRoute && shouldRefreshToken) {
                userRepo.saveToken(userRepo.getToken()!!.token, now + SECONDS_IN_A_DAY)
            }
            onSuccess(body)
            return success(body)
        } catch (t: Throwable) {
            Logger.e("generateJsonResponse") { t.message ?: "I frew up :(" }
            return failure("")
        }
    }

    protected suspend fun onBadResponse(
        res: HttpResponse,
        isProtectedRoute: Boolean = true,
    ): Either<String, Unit> {
        if (res.status >= HttpStatusCode.BadRequest) {
            if (res.status == HttpStatusCode.Unauthorized) {
                userRepo.clear()
                return failure(
                    if (!isProtectedRoute) {
                        "Invalid username or password"
                    } else {
                        "Your token is invalid or has expired"
                    },
                )
            }
            if (res.status == HttpStatusCode.BadGateway) {
                return failure("Bad gateway error. The server is most likely down.")
            }
            val err = res.body<ErrorMessage>().error
            if (err == BAD_TOKEN) {
                userRepo.clear()
            }
            return failure(err)
        }
        return success(Unit)
    }
}
