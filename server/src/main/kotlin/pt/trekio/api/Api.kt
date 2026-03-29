package pt.trekio.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import pt.trekio.errors.DomainError

typealias ControllerMethod = suspend RoutingContext.() -> Unit

interface Api {
    fun Int.toStatusCode() = HttpStatusCode.fromValue(this)

    suspend fun ApplicationCall.sendError(err: DomainError) {
        respond(HttpStatusCode.fromValue(err.statusCode), err.toErrorMessage())
    }
}
