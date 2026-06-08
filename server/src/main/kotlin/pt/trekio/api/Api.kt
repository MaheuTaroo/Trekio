package pt.trekio.api

import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingContext
import io.ktor.server.websocket.DefaultWebSocketServerSession
import pt.trekio.errors.DomainError
import pt.trekio.errors.UserError
import pt.trekio.server.config.sendError
import pt.trekio.server.config.closeWithError

typealias UserTokenPair = Pair<String, ULong>

val UserTokenPair.token get() = first
val UserTokenPair.userId get() = second

typealias ClassicControllerMethod = suspend RoutingContext.() -> Unit
typealias WebSocketControllerMethod = suspend DefaultWebSocketServerSession.() -> Unit

abstract class Api {
    protected companion object {
        const val DEFAULT_LIMIT = 10
    }

    protected fun protectedWithOAuth(
        handler: suspend RoutingContext.(OAuthAccessTokenResponse.OAuth2) -> Unit
    ): ClassicControllerMethod =
        suspend protected@{
            val auth = call.principal<OAuthAccessTokenResponse.OAuth2>()
            if (auth == null) {
                call.sendError(UserError.OAuthFailure)
                return@protected
            }

            handler(auth)
        }

    protected fun classicProtectedWithPair(
        handler: suspend RoutingContext.(UserTokenPair) -> Unit
    ): ClassicControllerMethod =
        suspend protected@{
            val auth = call.principal<UserTokenPair>()
            if (auth == null) {
                call.sendError(UserError.InvalidToken)
                return@protected
            }

            handler(auth)
        }

    protected fun classicProtectedWithId(handler: suspend RoutingContext.(ULong) -> Unit): ClassicControllerMethod =
        suspend protected@{
            val auth = call.principal<ULong>()
            if (auth == null) {
                call.sendError(UserError.InvalidToken)
                return@protected
            }

            handler(auth)
        }

    protected fun webSocketProtectedWithId(
        handler: suspend DefaultWebSocketServerSession.(ULong) -> Unit
    ): WebSocketControllerMethod =
        suspend protected@{
            val auth = call.principal<ULong>()
            if (auth == null) {
                closeWithError(UserError.InvalidToken)
                return@protected
            }

            handler(auth)
        }

    protected suspend fun RoutingContext.expectParameter(
        name: String,
        desc: String,
        block: suspend RoutingContext.(String) -> Unit,
    ) {
        val param = call.parameters[name]
        if (param == null) {
            call.sendError(DomainError.MissingParameter(desc))
            return
        }

        block(param)
    }

    protected suspend fun DefaultWebSocketServerSession.expectParameter(
        name: String,
        desc: String,
        block: suspend DefaultWebSocketServerSession.(String) -> Unit,
    ) {
        val param = call.parameters[name]
        if (param == null) {
            closeWithError(DomainError.MissingParameter(desc))
            return
        }

        block(param)
    }

    protected suspend fun RoutingContext.expectValidId(
        name: String,
        respectiveTo: String,
        block: suspend RoutingContext.(ULong) -> Unit,
    ) {
        expectParameter(name, "$respectiveTo ID") { param ->
            val id = param.toULongOrNull()
            if (id == null) {
                call.sendError(DomainError.MalformedParameter("unsigned long integer"))
                return@expectParameter
            }

            block(id)
        }
    }

    protected suspend fun DefaultWebSocketServerSession.expectValidId(
        name: String,
        respectiveTo: String,
        block: suspend DefaultWebSocketServerSession.(ULong) -> Unit,
    ) {
        expectParameter(name, "$respectiveTo ID") { param ->
            val id = param.toULongOrNull()
            if (id == null) {
                closeWithError(DomainError.MalformedParameter("unsigned long integer"))
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
