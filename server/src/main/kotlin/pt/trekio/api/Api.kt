package pt.trekio.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import pt.trekio.errors.DomainError
import pt.trekio.errors.UserError

typealias UserTokenPair = Pair<String, String>

val UserTokenPair.token get() = first
val UserTokenPair.username get() = second

typealias ControllerMethod = suspend RoutingContext.() -> Unit
typealias AuthenticatedControllerMethod = suspend RoutingContext.(UserTokenPair) -> Unit

abstract class Api {
    fun Int.toStatusCode() = HttpStatusCode.fromValue(this)

    suspend fun ApplicationCall.sendError(err: DomainError) {
        respond(HttpStatusCode.fromValue(err.statusCode), err.toErrorMessage())
    }

    protected fun protected(handler: AuthenticatedControllerMethod): ControllerMethod =
        suspend protected@{
            val auth = call.principal<UserTokenPair>()
            if (auth == null) {
                call.sendError(UserError.InvalidToken)
                return@protected
            }

            handler(auth)
        }
}
