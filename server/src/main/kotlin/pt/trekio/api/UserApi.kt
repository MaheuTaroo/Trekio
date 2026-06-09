package pt.trekio.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import pt.trekio.domain.User
import pt.trekio.domain.toDto
import pt.trekio.dto.UserCreateDto
import pt.trekio.dto.UserCredentialLogin
import pt.trekio.dto.UserList
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.misc.toDto
import pt.trekio.server.config.sendError
import pt.trekio.services.UserService

@Serializable
data class GoogleOAuthResponse(
    val id: String,
    val email: String,
    val verified_email: Boolean,
    val picture: String,
)

class UserApi(
    private val service: UserService,
) : Api() {
    fun createUser(): ClassicControllerMethod =
        suspend getUser@{
            val user = call.receive<UserCreateDto>()
            val res = service.createUser(user.username, user.email, user.password)
            if (res is Failure) {
                call.sendError(res.message)
                return@getUser
            }
            call.respond(HttpStatusCode.Created, (res as Success).value.toDto())
        }

    fun getUsers(): ClassicControllerMethod =
        classicProtectedWithId {
            paginate { skip, limit ->
                val res = service.getUsers(skip, limit)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@paginate
                }

                call.respond(UserList((res as Success).value.map(User::toDto)))
            }
        }

    fun getSelf(): ClassicControllerMethod =
        classicProtectedWithId {
            val res = service.getUserById(it)
            if (res is Failure) {
                call.sendError(res.message)
                return@classicProtectedWithId
            }

            call.respond((res as Success).value.toDto())
        }

    fun getUserByName(): ClassicControllerMethod =
        classicProtectedWithId {
            expectParameter("username", "username") { name ->
                val res = service.getUser(name)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectParameter
                }

                call.respond((res as Success).value.toDto())
            }
        }

    fun removeUser(): ClassicControllerMethod =
        classicProtectedWithPair {
            val res = service.deleteUser(it.token)
            if (res is Failure) {
                call.sendError(res.message)
                return@classicProtectedWithPair
            }

            call.respond(HttpStatusCode.NoContent)
        }

    fun logUserIn(): ClassicControllerMethod =
        suspend logUserIn@{
            val credentials = call.receive<UserCredentialLogin>()

            val res = service.createTokenFor(credentials.email, credentials.password)
            if (res is Failure) {
                call.sendError(res.message)
                return@logUserIn
            }
            call.respond(HttpStatusCode.Created, (res as Success).value.toDto())
        }

    fun logUserOut(): ClassicControllerMethod =
        classicProtectedWithPair {
            val res = service.revokeToken(it.token)
            if (res is Failure) {
                call.sendError(res.message)
                return@classicProtectedWithPair
            }

            call.respond(HttpStatusCode.NoContent)
        }

    fun refreshToken(): ClassicControllerMethod =
        classicProtectedWithPair {
            val res = service.refreshToken(it.userId)
            if (res is Failure) {
                call.sendError(res.message)
                return@classicProtectedWithPair
            }

            call.respond(HttpStatusCode.OK, (res as Success).value.toDto())
        }

    fun oauthAuthentication(httpClient: HttpClient): ClassicControllerMethod =
        protectedWithOAuth {
            val email = fetchGoogleUserInfo(httpClient, it.accessToken)
            if (email is Failure) {
                call.sendError(email.message)
                return@protectedWithOAuth
            }
            val res = service.oauthService((email as Success).value)
            if (res is Failure) {
                call.sendError(res.message)
                return@protectedWithOAuth
            }
            call.respond(HttpStatusCode.Created, (res as Success).value.toDto())
        }

    private suspend fun fetchGoogleUserInfo(
        httpClient: HttpClient,
        accessToken: String,
    ): Either<UserError, String> {
        val response =
            httpClient.get("https://www.googleapis.com/oauth2/v2/userinfo") {
                header("Authorization", "Bearer $accessToken")
            }

        return if (response.status == HttpStatusCode.OK) {
            success(response.body<GoogleOAuthResponse>().email)
        } else {
            failure(UserError.OAuthGetInfoFailure)
        }
    }
}
