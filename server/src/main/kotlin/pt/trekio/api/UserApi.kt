package pt.trekio.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import pt.trekio.domain.User
import pt.trekio.domain.toDto
import pt.trekio.dto.ErrorMessage
import pt.trekio.dto.UserCreate
import pt.trekio.dto.UserCredentialLogin
import pt.trekio.dto.UserList
import pt.trekio.errors.UserError
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.misc.toDto
import pt.trekio.services.UserService

class UserApi(
    private val service: UserService,
) : Api {
    fun createUser(): ControllerMethod =
        suspend createUser@{
            try {
                val user = call.receive<UserCreate>()
                val res = service.createUser(user.username, user.email, user.password)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@createUser
                }
                call.respond((res as Success).value.toDto())
            } catch (_: Exception) {
                call.respond(
                    ErrorMessage("Request body is missing or malformed; check /docs or /documentation.html on how to use the Trekio API"),
                )
            }
        }

    fun getUsers(): ControllerMethod =
        suspend getUsers@{
            val skip = call.queryParameters["skip"]?.toIntOrNull() ?: 0
            val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 10

            val res = service.getUsers(skip, limit)
            if (res is Failure) {
                call.sendError(res.message)
                return@getUsers
            }
            call.respond(UserList((res as Success).value.map(User::toDto)))
        }

    fun getSelf(): ControllerMethod =
        suspend getSelf@{
            val token = call.request.header("Authorization")
            println("Authorization: $token")
            if (token == null) {
                call.sendError(UserError.MissingToken)
                return@getSelf
            }

            val res = service.getOwnDetails(token)
            if (res is Failure) {
                call.sendError(res.message)
                return@getSelf
            }
            call.respond((res as Success).value.toDto())
        }

    fun getUserByName(): ControllerMethod =
        suspend getUserByName@{
            val name = call.pathParameters["name"] as String
            val res = service.getUser(name)
            if (res is Failure) {
                call.sendError(res.message)
                return@getUserByName
            }
            call.respond((res as Success).value.toDto())
        }

    fun removeUser(): ControllerMethod =
        suspend removeUser@{
            val token = call.request.header("Authorization")
            if (token == null) {
                call.respond(UserError.MissingToken)
                return@removeUser
            }

            val res = service.deleteUser(token)
            if (res is Failure) {
                call.sendError(res.message)
                return@removeUser
            }
            call.respond(HttpStatusCode.OK)
        }

    fun logUserIn(): ControllerMethod =
        suspend logUserIn@{
            val credentials = call.receive<UserCredentialLogin>()

            val res = service.createTokenFor(credentials.email, credentials.password)
            if (res is Failure) {
                call.sendError(res.message)
                return@logUserIn
            }
            call.respond(HttpStatusCode.Created, (res as Success).value.toDto())
        }

    fun logUserOut(): ControllerMethod =
        suspend logUserOut@{
            val token = call.request.header("Authorization")
            if (token == null) {
                call.sendError(UserError.MissingToken)
                return@logUserOut
            }
            val res = service.revokeToken(token)
            if (res is Failure) {
                call.sendError(res.message)
                return@logUserOut
            }
            call.respond(HttpStatusCode.OK)
        }
}
