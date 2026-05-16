package pt.trekio.services.user

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.path
import pt.trekio.BASE_URL
import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.dto.UserCreate
import pt.trekio.dto.UserCredentialLogin
import pt.trekio.dto.UserDto
import pt.trekio.misc.Either
import pt.trekio.misc.success
import pt.trekio.repos.UserRepo
import pt.trekio.services.Service

class UserHttpService(
    val webClient: HttpClient,
    userRepo: UserRepo,
) : Service(userRepo),
    UserService {
    companion object {
        private const val ENDPOINT = "/users"
        private const val OAUTH = "oauth"
    }

    private suspend fun updateUserData(
        token: String,
        expiration: Long,
        email: String,
    ) {
        userRepo.saveToken(token, expiration, email)
    }

    override suspend fun signUp(
        username: String,
        email: String,
        password: String,
    ): Either<String, TokenExternalInfoDto> =
        generateJsonResponse<TokenExternalInfoDto>({
            webClient.post {
                url.path("$ENDPOINT/create")
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody<UserCreate>(UserCreate(username, email, password))
            }
        }, isProtectedRoute = false, shouldRefreshToken = false, onSuccess = {
            updateUserData(it.refreshTokenValue, it.tokenExpiration, email)
        })

    override suspend fun login(
        email: String,
        password: String,
    ): Either<String, TokenExternalInfoDto> =
        generateJsonResponse<TokenExternalInfoDto>({
            webClient.post {
                url.path("$ENDPOINT/login")
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody<UserCredentialLogin>(UserCredentialLogin(email, password))
            }
        }, isProtectedRoute = false, shouldRefreshToken = false, onSuccess = {
            updateUserData(it.refreshTokenValue, it.tokenExpiration, email)
        })

    override suspend fun getDetails(): Either<String, UserDto> =
        generateJsonResponse<UserDto>(
            {
                webClient.get {
                    url.path("$ENDPOINT/self")
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    Logger.i("UserHttpService") { "Getting user details with token: $it" }
                    headers {
                        append("Authorization", "Bearer $it")
                    }
                }
            }, onSuccess = {})

    override suspend fun delete(): Either<String, Unit> =
        generateJsonResponse<Unit>({
            webClient.delete {
                url.path("$ENDPOINT/delete")
                headers {
                    append("Authorization", "bearer $it")
                }
            }
        }, shouldRefreshToken = false) {
            userRepo.clear()
        }

    override suspend fun googlePopup(): Either<String, String> =
        success("$BASE_URL$ENDPOINT/$OAUTH/google")

    override suspend fun googleCallback() {
        TODO("Not yet implemented")
    }
}
