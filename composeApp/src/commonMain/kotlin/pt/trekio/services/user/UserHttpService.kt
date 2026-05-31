package pt.trekio.services.user

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.path
import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.dto.UserCreateDto
import pt.trekio.dto.UserCredentialLogin
import pt.trekio.dto.UserDto
import pt.trekio.misc.ApiRoutes.UserCreate
import pt.trekio.misc.ApiRoutes.UserDelete
import pt.trekio.misc.ApiRoutes.UserLogin
import pt.trekio.misc.ApiRoutes.UserOauthLogin
import pt.trekio.misc.ApiRoutes.UserSelf
import pt.trekio.misc.Either
import pt.trekio.misc.Routes
import pt.trekio.misc.success
import pt.trekio.repos.UserRepo
import pt.trekio.services.Service

class UserHttpService(
    webClient: HttpClient,
    userRepo: UserRepo,
) : Service(userRepo, webClient),
    UserService {
    private suspend fun updateUserData(
        accessToken: String,
        refreshToken: String,
        expiration: Long,
        email: String? = null,
    ) = userRepo.saveToken(accessToken, refreshToken, expiration, email)

    override suspend fun signUp(
        username: String,
        email: String,
        password: String,
    ): Either<String, TokenExternalInfoDto> =
        generateJsonResponse<TokenExternalInfoDto>({ _, route ->
            webClient.post {
                url.path(route)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody<UserCreateDto>(UserCreateDto(username, email, password))
            }
        }, route = UserCreate, onSuccess = {
            updateUserData(it.accessTokenValue, it.refreshTokenValue, it.tokenExpiration, email)
        })

    override suspend fun login(
        email: String,
        password: String,
    ): Either<String, TokenExternalInfoDto> =
        generateJsonResponse<TokenExternalInfoDto>({ _, route ->
            webClient.post {
                url.path(route)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody<UserCredentialLogin>(UserCredentialLogin(email, password))
            }
        }, route = UserLogin, onSuccess = {
            updateUserData(it.accessTokenValue, it.refreshTokenValue, it.tokenExpiration, email)
        })

    override suspend fun getDetails(): Either<String, UserDto> =
        generateJsonResponse<UserDto>(
            { token, route ->
                webClient.get {
                    url.path(route)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    headers {
                        bearerAuth(token)
                    }
                }
            },
            route = UserSelf,
            onSuccess = {},
        )

    override suspend fun delete(): Either<String, Unit> =
        generateJsonResponse<Unit>({ token, route ->
            webClient.delete {
                url.path(route)
                headers {
                    bearerAuth(token)
                }
            }
        }, route = UserDelete) {
            userRepo.clear()
        }

    override suspend fun googlePopup(): Either<String, String> = success(Routes.BASE_URL + UserOauthLogin.path)

    override suspend fun googleCallback() {
        TODO("Not yet implemented")
    }
}
