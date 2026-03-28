package pt.trekio.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.RoutingContext

typealias ControllerMethod = suspend RoutingContext.() -> Unit

interface Api {
    fun Int.toStatusCode() = HttpStatusCode.fromValue(this)
}
