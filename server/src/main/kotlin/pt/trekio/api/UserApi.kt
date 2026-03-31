package pt.trekio.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import pt.trekio.domain.User
import pt.trekio.domain.toDto
import pt.trekio.dto.ErrorMessage
import pt.trekio.dto.UserCreate
import pt.trekio.dto.UserCredentialLogin
import pt.trekio.dto.UserList
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.misc.toDto
import pt.trekio.services.UserService

class UserApi(
    private val service: UserService,
) : Api() {
    /**
     * Registers a new user.
     *
     * Body: json [UserCreate] the new user's information.
     *
     * Responses:
     *   – 201 [pt.trekio.dto.TokenExternalInfoDto] The newly created user's token.
     *   – 400 Either the username, email or password is not following the correct specifications.
     *   – 401 Authentication failed for the current user.
     *   – 409 There already is a user with said username or email.
     */
    fun createUser(): ControllerMethod =
        suspend getUser@{
            try {
                val user = call.receive<UserCreate>()
                val res = service.createUser(user.username, user.email, user.password)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@getUser
                }
                call.respond(HttpStatusCode.Created, (res as Success).value.toDto())
            } catch (_: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorMessage(
                        "Request body is missing or malformed; check /docs or /documentation.html on how to use the Trekio API",
                    ),
                )
            }
        }

    /**
     * Fetches a page of users following skipping and limiting values.
     *
     * Security: trekio-bearer
     *
     * Query: skip [Int] the amount of users to skip.
     *        limit [Int] the amount of users to fetch.
     *
     * Responses:
     *   – 200 [UserList] The paginated list of users.
     *   – 400 Either the 'skip' amount is negative, or the 'limit' amount is lower than 1.
     *   – 401 Authentication failed for the current user.
     */
    fun getUsers(): ControllerMethod =
        protected {
            val skip = call.queryParameters["skip"]?.toIntOrNull() ?: 0
            val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 10

            val res = service.getUsers(skip, limit)
            if (res is Failure) {
                call.sendError(res.message)
                return@protected
            }

            call.respond(UserList((res as Success).value.map(User::toDto)))
        }

    /**
     * Fetches the current user's details.
     *
     * Security: trekio-bearer
     *
     * Responses:
     *   – 200 [UserList] The user's own details.
     *   – 401 Authentication failed for the current user.
     */
    fun getSelf(): ControllerMethod =
        protected {
            val res = service.getOwnDetails(it.token)
            if (res is Failure) {
                call.sendError(res.message)
                return@protected
            }

            call.respond((res as Success).value.toDto())
        }

    /**
     * Fetches a page of users following skipping and limiting values.
     *
     * Security: trekio-bearer
     *
     * Path: [String] name the name of the user to fetch.
     *
     * Responses:
     *   – 200 [UserList] The paginated list of users.
     *   – 400 The username is not following the specifications.
     *   – 401 Authentication failed for the current user.
     */
    fun getUserByName(): ControllerMethod =
        protected {
            val name = call.pathParameters["name"] as String

            val res = service.getUser(name)
            if (res is Failure) {
                call.sendError(res.message)
                return@protected
            }

            call.respond((res as Success).value.toDto())
        }

    /**
     * Removes the user's own account.
     *
     * Security: trekio-bearer
     *
     * Responses:
     *   – 200 [UserList] The paginated list of users.
     *   – 401 Authentication failed for the current user.
     */
    fun removeUser(): ControllerMethod =
        protected {
            val res = service.deleteUser(it.token)
            if (res is Failure) {
                call.sendError(res.message)
                return@protected
            }

            call.respond(HttpStatusCode.OK)
        }

    /**
     * Logs a user in.
     *
     * Security: trekio-bearer
     *
     * Body: json [UserCredentialLogin] the user's email and password.
     *
     * Responses:
     *   – 201 [UserList] The user's token.
     *   – 400 The email is not following the correct specifications.
     *   – 403 The password is incorrect.
     *   – 404 There is no user connected to the given email.
     */
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

    /**
     * Logs a user out.
     *
     * Security: trekio-bearer
     *
     * Responses:
     *   – 200 [Unit]
     *   – 401 The token does not exist.
     */
    fun logUserOut(): ControllerMethod =
        protected {
            val res = service.revokeToken(it.token)
            if (res is Failure) {
                call.sendError(res.message)
                return@protected
            }

            call.respond(HttpStatusCode.OK)
        }
}
