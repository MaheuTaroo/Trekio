package pt.trekio.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import pt.trekio.domain.User
import pt.trekio.domain.toDto
import pt.trekio.dto.UserCreate
import pt.trekio.dto.UserCredentialLogin
import pt.trekio.dto.UserList
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.misc.toDto
import pt.trekio.server.config.sendError
import pt.trekio.services.UserService

class UserApi(
    private val service: UserService,
) : Api() {
    fun createUser(): ControllerMethod =
        suspend getUser@{
            val user = call.receive<UserCreate>()
            val res = service.createUser(user.username, user.email, user.password)
            if (res is Failure) {
                call.sendError(res.message)
                return@getUser
            }
            call.respond(HttpStatusCode.Created, (res as Success).value.toDto())
        }

    fun getUsers(): ControllerMethod =
        protectedWithId {
            paginate { skip, limit ->
                val res = service.getUsers(skip, limit)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@paginate
                }

                call.respond(UserList((res as Success).value.map(User::toDto)))
            }
        }

    fun getSelf(): ControllerMethod =
        protectedWithId {
            val res = service.getUserById(it)
            if (res is Failure) {
                call.sendError(res.message)
                return@protectedWithId
            }

            call.respond((res as Success).value.toDto())
        }

    fun getUserByName(): ControllerMethod =
        protectedWithId {
            expectParameter("username", "username") { name ->
                val res = service.getUser(name)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectParameter
                }

                call.respond((res as Success).value.toDto())
            }
        }

    fun removeUser(): ControllerMethod =
        protectedWithPair {
            val res = service.deleteUser(it.token)
            if (res is Failure) {
                call.sendError(res.message)
                return@protectedWithPair
            }

            call.respond(HttpStatusCode.NoContent)
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
        protectedWithPair {
            val res = service.revokeToken(it.token)
            if (res is Failure) {
                call.sendError(res.message)
                return@protectedWithPair
            }

            call.respond(HttpStatusCode.NoContent)
        }

    fun refreshToken(): ControllerMethod =
        protectedWithPair {
            val res = service.refreshToken(it.userId)
            if (res is Failure) {
                call.sendError(res.message)
                return@protectedWithPair
            }

            call.respond(HttpStatusCode.OK, (res as Success).value.toDto())
        }
}
