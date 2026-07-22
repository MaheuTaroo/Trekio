package pt.trekio.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pt.trekio.domain.User
import pt.trekio.domain.toDto
import pt.trekio.dto.OAuthCodeDto
import pt.trekio.dto.UserCreateDto
import pt.trekio.dto.UserCredentialLogin
import pt.trekio.dto.UserList
import pt.trekio.dto.UserUpdateDto
import pt.trekio.errors.UserError
import pt.trekio.misc.ApiRoutes.DeepLink
import pt.trekio.misc.Either
import pt.trekio.misc.Failure
import pt.trekio.misc.OAuthCode
import pt.trekio.misc.Routes.CODE
import pt.trekio.misc.Routes.EMAIL
import pt.trekio.misc.Routes.ERROR
import pt.trekio.misc.Routes.NEW
import pt.trekio.misc.Routes.OAUTH_INFO
import pt.trekio.misc.Routes.USERNAME
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
    @SerialName("verified_email")
    val verifiedEmail: Boolean,
    val picture: String,
)

class UserApi(
    private val service: UserService,
) : Api() {
    fun createUser(): ClassicControllerMethod =
        suspend createUser@{
            val user = call.receive<UserCreateDto>()
            val res = service.createUser(user.username, user.email, user.password)
            if (res is Failure) {
                call.sendError(res.message)
                return@createUser
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

    fun getUserByIdentifier(): ClassicControllerMethod =
        classicProtectedWithId {
            expectParameter("identifier", "username/ID") { identifier ->
                val res =
                    when {
                        identifier.isBlank() -> {
                            call.sendError(UserError.InvalidIdentifier)
                            return@expectParameter
                        }

                        identifier.first().isLetter() -> service.getUser(identifier)

                        else -> {
                            val id = identifier.toULongOrNull()
                            if (id == null || id == 0uL) {
                                call.sendError(UserError.InvalidIdentifier)
                                return@expectParameter
                            }
                            service.getUserById(id)
                        }
                    }

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

            call.respond(HttpStatusCode.NoContent, (res as Success).value)
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

            call.respond(HttpStatusCode.NoContent, (res as Success).value)
        }

    fun update(): ClassicControllerMethod =
        classicProtectedWithPair {
            val body = call.receive<UserUpdateDto>()
            val res = service.updateUser(body.username, body.password, it.userId)
            if (res is Failure) {
                call.sendError(res.message)
                return@classicProtectedWithPair
            }

            call.respond(HttpStatusCode.Created, (res as Success).value.toDto())
        }

    fun refreshToken(): ClassicControllerMethod =
        classicProtectedWithPair {
            val res = service.refreshToken(it.userId)
            if (res is Failure) {
                call.sendError(res.message)
                return@classicProtectedWithPair
            }

            call.respond((res as Success).value.toDto())
        }

    fun oauthAuthentication(httpClient: HttpClient): ClassicControllerMethod =
        protectedWithOAuth {
            val deepLink =
                when (val email = fetchGoogleUserInfo(httpClient, it.accessToken)) {
                    is Failure -> errorDeepLink(email.message.message)
                    is Success ->
                        when (val res = service.oauthService(email.value)) {
                            is Failure -> errorDeepLink(res.message.message)
                            is Success -> successDeepLink(res.value)
                        }
                }
            call.respondRedirect(deepLink, permanent = false)
        }

    private fun errorDeepLink(message: String) =
        URLBuilder(DeepLink.path)
            .apply { parameters.append(ERROR, message) }
            .buildString()

    private fun successDeepLink(result: Pair<OAuthCode, Boolean>) =
        URLBuilder(DeepLink.path)
            .apply {
                parameters.append(CODE, result.first.code)
                parameters.append(EMAIL, result.first.email.value)
                parameters.append(USERNAME, result.first.username.value)
                parameters.append(NEW, result.second.toString())
            }.buildString()

    fun oauthCodeVerifier(): ClassicControllerMethod =
        suspend oauthCodeVerifier@{
            val oauthCode = call.receive<OAuthCodeDto>()
            val res = service.oauthVerifyCode(oauthCode.email, oauthCode.username, oauthCode.code)
            if (res is Failure) {
                call.sendError(res.message)
                return@oauthCodeVerifier
            }

            call.respond(HttpStatusCode.Created, (res as Success).value.toDto())
        }

    private suspend fun fetchGoogleUserInfo(
        httpClient: HttpClient,
        accessToken: String,
    ): Either<UserError, String> {
        val response =
            httpClient.get(OAUTH_INFO) {
                header("Authorization", "Bearer $accessToken")
            }

        return if (response.status == HttpStatusCode.OK) {
            success(response.body<GoogleOAuthResponse>().email)
        } else {
            failure(UserError.OAuthGetInfoFailure)
        }
    }
}
