package pt.trekio.services.user

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.path
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import pt.trekio.dto.StatisticsDto
import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.dto.UserCreateDto
import pt.trekio.dto.UserCredentialLogin
import pt.trekio.dto.UserDto
import pt.trekio.dto.UserUpdateDto
import pt.trekio.misc.ApiRoutes
import pt.trekio.misc.ApiRoutes.UserCreate
import pt.trekio.misc.ApiRoutes.UserDelete
import pt.trekio.misc.ApiRoutes.UserLogin
import pt.trekio.misc.ApiRoutes.UserOauthLogin
import pt.trekio.misc.ApiRoutes.UserSelf
import pt.trekio.misc.Either
import pt.trekio.misc.Routes
import pt.trekio.misc.success
import pt.trekio.repos.UserRepository
import pt.trekio.services.Service

class UserHttpService(
    userRepo: UserRepository,
    webClient: HttpClient,
) : Service(userRepo, webClient),
    UserService {
    private suspend fun updateUserTokens(
        accessToken: String,
        refreshToken: String,
        expiration: Long,
        email: String? = null,
    ) = userRepo.saveToken(accessToken, refreshToken, expiration, email)

    private suspend fun updateUserDetails(
        id: ULong?,
        username: String,
        rank: String?,
    ) = userRepo.saveOwnDetails(id, username, rank)

    override suspend fun signUp(
        username: String,
        email: String,
        password: String,
    ): Either<String, TokenExternalInfoDto> =
        generateJsonResponse<TokenExternalInfoDto>(UserCreate, { route, _ ->
            post {
                url.path(route)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(UserCreateDto(username, email, password))
            }
        }) {
            updateUserTokens(
                it.accessTokenValue,
                it.refreshTokenValue,
                it.tokenExpiration,
                email,
            )
            getSelfDetails()
        }

    override suspend fun login(
        email: String,
        password: String,
    ): Either<String, TokenExternalInfoDto> =
        generateJsonResponse<TokenExternalInfoDto>(UserLogin, { route, _ ->
            post {
                url.path(route)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(UserCredentialLogin(email, password))
            }
        }) {
            updateUserTokens(
                it.accessTokenValue,
                it.refreshTokenValue,
                it.tokenExpiration,
                email,
            )
            getSelfDetails()
        }

    override suspend fun logout(): Either<String, Unit> =
        generateJsonResponse<Unit>(ApiRoutes.UserLogout, { route, token ->
            delete {
                url.path(route)
                headers {
                    bearerAuth(token)
                }
            }
        }) {
            MainScope().launch {
                userRepo.clear()
            }
        }

    override suspend fun getSelfDetails(): Either<String, UserDto> =
        generateJsonResponse<UserDto>(
            UserSelf,
            { route, token ->
                get {
                    url.path(route)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    headers {
                        bearerAuth(token)
                    }
                }
            },
        ) { updateUserDetails(it.id, it.username, it.rank) }

    override suspend fun getStatsOf(id: ULong): Either<String, StatisticsDto> =
        generateJsonResponse(ApiRoutes.HikeUserStats(id), { route, token ->
            get {
                url.path(route)
                accept(ContentType.Application.Json)
                headers {
                    bearerAuth(token)
                }
            }
        }) { }

    override suspend fun updateDetails(
        username: String?,
        password: String?,
    ): Either<String, TokenExternalInfoDto> =
        generateJsonResponse<TokenExternalInfoDto>(ApiRoutes.UserUpdate, { route, token ->
            put {
                url.path(route)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(UserUpdateDto(username, password))
                headers {
                    bearerAuth(token)
                }
            }
        }) {
            updateUserTokens(
                it.accessTokenValue,
                it.refreshTokenValue,
                it.tokenExpiration,
            )
            username?.let { updateUserDetails(null, username, null) }
        }

    override suspend fun deleteUser(): Either<String, Unit> =
        generateJsonResponse<Unit>(UserDelete, { route, token ->
            delete {
                url.path(route)
                headers {
                    bearerAuth(token)
                }
            }
        }) {
            MainScope().launch {
                userRepo.clear()
            }
        }

    override suspend fun googlePopup(): Either<String, String> = success(Routes.BASE_URL + UserOauthLogin.path)

    override suspend fun googleCallback() {
        TODO("Not yet implemented")
    }
}
