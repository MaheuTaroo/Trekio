package pt.trekio.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import pt.trekio.errors.DomainError
import pt.trekio.errors.UserError
import pt.trekio.misc.Username

typealias UserTokenPair = Pair<String, Username>

val UserTokenPair.token get() = first
val UserTokenPair.username get() = second

typealias ControllerMethod = suspend RoutingContext.() -> Unit

abstract class Api {
    protected companion object {
        const val DEFAULT_LIMIT = 10
    }

    suspend fun ApplicationCall.sendError(err: DomainError) {
        respond(HttpStatusCode.fromValue(err.statusCode), err.toErrorMessage())
    }

    protected fun protected(handler: suspend RoutingContext.(UserTokenPair) -> Unit): ControllerMethod =
        suspend protected@{
            val auth = call.principal<UserTokenPair>()
            if (auth == null) {
                call.sendError(UserError.InvalidToken)
                return@protected
            }

            handler(auth)
        }

    protected suspend fun RoutingContext.expectParameter(
        name: String,
        block: suspend RoutingContext.(String) -> Unit,
    ) {
        val param = call.parameters[name]
        if (param == null) {
            call.sendError(DomainError.MissingParameter(name))
            return
        }

        block(param)
    }

    protected suspend fun RoutingContext.expectValidId(
        name: String,
        block: suspend RoutingContext.(ULong) -> Unit,
    ) {
        expectParameter(name) { param ->
            val id = param.toULongOrNull()
            if (id == null) {
                call.sendError(DomainError.MalformedParameter("unsigned long integer"))
                return@expectParameter
            }

            block(id)
        }
    }

    protected suspend fun RoutingContext.paginate(block: suspend RoutingContext.(Int, Int) -> Unit) {
        val skip = call.queryParameters["skip"]?.toIntOrNull() ?: 0
        val limit = call.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT

        block(skip, limit)
    }
}
