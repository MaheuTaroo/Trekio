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
import pt.trekio.misc.Either
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.misc.toDto
import pt.trekio.services.UserService

class UserApi(private val service: UserService): Api {
    fun createUser(): ControllerMethod = suspend {
        try {
            val user = call.receive<UserCreate>()
            val res = service.createUser(user.username, user.email, user.password)
            if (res is Either.Failure) {
                call.respond(HttpStatusCode.BadRequest, ErrorMessage(res.message.error))
            } else {
                call.respond((res as Either.Success).value.toDto())
            }
        } catch (_: Exception) {
            call.respond(
                ErrorMessage("Request body is missing or malformed; check /docs or /documentation.html on how to use the Trekio API"),
            )
        }
    }

    fun getUsers(): ControllerMethod = suspend {
        val skip = call.queryParameters["skip"]?.toIntOrNull() ?: 0
        val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 10

        val res = service.getUsers(skip, limit)
        if (res is Failure) {
            call.respond(HttpStatusCode.BadRequest, res.message.toErrorMessage())
        } else {
            call.respond(UserList((res as Success).value.map(User::toDto)))
        }
    }

    fun getSelf(): ControllerMethod = suspend getSelf@{
        val token = call.request.header("Authorization")
        println("Authorization: $token")
        if (token == null) {
            call.respond(HttpStatusCode.Unauthorized, ErrorMessage(UserError.MissingToken.error))
            return@getSelf
        }

        val res = service.getOwnDetails(token)
        if (res is Failure) {
            call.respond(HttpStatusCode.BadRequest, ErrorMessage(res.message.error))
        } else {
            call.respond((res as Success).value.toDto())
        }
    }

    fun getUserByName(): ControllerMethod = suspend {
        val name = call.pathParameters["name"] as String
        val res = service.getUser(name)
        if (res is Failure) {
            call.respond(HttpStatusCode.BadRequest, ErrorMessage(res.message.error))
        } else {
            call.respond((res as Success).value.toDto())
        }
    }

    fun removeUser(): ControllerMethod = suspend removeUser@{
        val token = call.request.header("Authorization")
        if (token == null) {
            call.respond(ErrorMessage(UserError.MissingToken.error))
            return@removeUser
        }

        val res = service.deleteUser(token)
        if (res is Failure) {
            call.respond(HttpStatusCode.BadRequest, ErrorMessage(res.message.error))
        } else {
            call.respond(HttpStatusCode.OK)
        }
    }

    fun logUserIn(): ControllerMethod = suspend logUserIn@{
        val credentials = call.receive<UserCredentialLogin>()

        val res = service.createTokenFor(credentials.email, credentials.password)
        if (res is Failure) {
            call.respond(HttpStatusCode.BadRequest, res.message.toErrorMessage())
            return@logUserIn
        }
        call.respond(HttpStatusCode.Created, (res as Success).value.toDto())
    }

    fun logUserOut(): ControllerMethod = suspend logUserOut@{
        val token = call.request.header("Authorization")
        if (token == null) {
            call.respond(HttpStatusCode.Unauthorized, UserError.MissingToken.toErrorMessage())
            return@logUserOut
        }
        val res = service.revokeToken(token)
        if (res is Either.Failure) {
            call.respond(HttpStatusCode.BadRequest, ErrorMessage(res.message.error))
            return@logUserOut
        }
        call.respond(HttpStatusCode.OK)
    }
}