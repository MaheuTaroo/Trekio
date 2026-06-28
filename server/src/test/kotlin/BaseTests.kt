import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import pt.trekio.dto.ErrorMessage
import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.dto.UserCreateDto
import pt.trekio.dto.UserCredentialLogin
import pt.trekio.dto.UserDto
import pt.trekio.dto.UserList
import pt.trekio.redis.RedisService
import pt.trekio.repos.mem.HikeMemoryRepository
import pt.trekio.repos.mem.TrailMemoryRepository
import pt.trekio.repos.mem.UserMemoryRepository
import pt.trekio.server.configureTrekio
import kotlin.test.AfterTest
import kotlin.test.assertEquals

interface BaseTests {
    fun testRequests(block: suspend (client: HttpClient) -> Unit) =
        testApplication {
            application {
                configureTrekio(
                    UserMemoryRepository,
                    TrailMemoryRepository,
                    HikeMemoryRepository,
                    RedisService(System.getenv("REDIS_URL") ?: "redis://localhost:6379"),
                )
            }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                prettyPrint = true
                                isLenient = true
                                ignoreUnknownKeys = true
                            },
                        )
                    }
                }

            block(client)
        }

    suspend fun <T> runCatchingExpected(
        expectedStatus: HttpStatusCode? = null,
        block: suspend () -> T,
    ) = runCatching {
        block()
    }.onFailure { ex ->
        val assertedStatus =
            (ex as? AssertionError)
                ?.message
                ?.let {
                    Regex("""but was:<(\d+)""")
                        .find(it)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull()
                }

        if (expectedStatus != null) {
            if (assertedStatus == expectedStatus.value) {
                return@onFailure
            }
            throw ex
        } else {
            when (ex) {
                is ClientRequestException if ex.response.status.value in 400..499 -> Unit
                is AssertionError if assertedStatus in 400..499 -> Unit
                else -> throw ex
            }
        }
    }

    suspend fun assertFailure(
        client: HttpClient,
        method: HttpMethod,
        route: String,
        token: String? = null,
        expectedStatus: HttpStatusCode = HttpStatusCode.BadRequest,
        expectedError: ErrorMessage,
        block: HttpRequestBuilder.() -> Unit = { },
    ) {
        val response =
            client.request(route) {
                this.method = method
                token?.let { bearerAuth(it) }
                block()
            }
        assertEquals(expectedStatus, response.status)
        assertEquals(expectedError, response.body<ErrorMessage>())
    }

    suspend fun assertStatus(
        client: HttpClient,
        method: HttpMethod,
        route: String,
        token: String? = null,
        expectedStatus: HttpStatusCode,
    ) {
        val response =
            client.request(route) {
                this.method = method
                token?.let { bearerAuth(it) }
            }
        assertEquals(expectedStatus, response.status)
    }

    interface Users : BaseTests {
        companion object {
            const val URL = "/users"
            const val CREATE_URL = "$URL/create"
            const val LOGIN_URL = "$URL/login"
            const val DELETE_URL = "$URL/delete"
            const val GET_URL = URL
            const val SELF_URL = "$URL/self"
            const val LOGOUT_URL = "$URL/logout"
            const val REFRESH_URL = "$URL/refresh"

            fun getByNameUrl(username: String) = "$URL/$username"
        }

        @AfterTest
        fun cleanup() {
            runBlocking {
                UserMemoryRepository.deleteAllUsers()
            }
        }

        suspend fun createUser(
            client: HttpClient,
            username: String = "John_Doe",
            email: String = "john.doe@gmail.com",
            password: String = "JohnDoe123#",
        ): TokenExternalInfoDto =
            client.apiRequest(
                HttpMethod.Post,
                CREATE_URL,
                expectedStatus = HttpStatusCode.Created,
            ) { setBody(UserCreateDto(username, email, password)) }

        suspend fun createUserFailure(
            client: HttpClient,
            expectedError: ErrorMessage,
            expectedStatus: HttpStatusCode = HttpStatusCode.BadRequest,
            block: HttpRequestBuilder.() -> Unit = { contentType(ContentType.Application.Json) },
        ) = assertFailure(
            client,
            HttpMethod.Post,
            CREATE_URL,
            expectedStatus = expectedStatus,
            expectedError = expectedError,
            block = block,
        )

        suspend fun getUsers(
            client: HttpClient,
            token: String,
            skip: Int? = null,
            limit: Int? = null,
        ): UserList {
            val params =
                buildList {
                    skip?.let { add("skip=$it") }
                    limit?.let { add("limit=$it") }
                }.joinToString("&")
            val route = if (params.isEmpty()) GET_URL else "$GET_URL?$params"
            return client.apiRequest(HttpMethod.Get, route, token)
        }

        suspend fun getUsersFailure(
            client: HttpClient,
            token: String? = null,
            skip: Int? = null,
            limit: Int? = null,
            expectedError: ErrorMessage? = null,
            expectedStatus: HttpStatusCode = HttpStatusCode.BadRequest,
        ) {
            val params =
                buildList {
                    skip?.let { add("skip=$it") }
                    limit?.let { add("limit=$it") }
                }.joinToString("&")
            val route = if (params.isEmpty()) GET_URL else "$GET_URL?$params"

            if (expectedError != null) {
                assertFailure(client, HttpMethod.Get, route, token, expectedStatus, expectedError)
            } else {
                assertStatus(client, HttpMethod.Get, route, token, expectedStatus)
            }
        }

        suspend fun getSelf(
            client: HttpClient,
            token: String,
        ): UserDto = client.apiRequest(HttpMethod.Get, SELF_URL, token)

        suspend fun getSelfFailure(
            client: HttpClient,
            token: String,
            expectedStatus: HttpStatusCode = HttpStatusCode.Forbidden,
        ) = assertStatus(client, HttpMethod.Get, SELF_URL, token, expectedStatus)

        suspend fun getUserByName(
            client: HttpClient,
            token: String,
            username: String,
        ): UserDto = client.apiRequest(HttpMethod.Get, getByNameUrl(username), token)

        suspend fun getUserByNameFailure(
            client: HttpClient,
            token: String,
            username: String,
            expectedError: ErrorMessage? = null,
            expectedStatus: HttpStatusCode = HttpStatusCode.BadRequest,
        ) = if (expectedError != null) {
            assertFailure(client, HttpMethod.Get, getByNameUrl(username), token, expectedStatus, expectedError)
        } else {
            assertStatus(client, HttpMethod.Get, getByNameUrl(username), token, expectedStatus)
        }

        suspend fun removeUser(
            client: HttpClient,
            token: String,
        ): Unit = client.apiRequest(HttpMethod.Delete, DELETE_URL, token, HttpStatusCode.NoContent)

        suspend fun removeUserFailure(
            client: HttpClient,
            token: String,
            expectedStatus: HttpStatusCode = HttpStatusCode.Unauthorized,
        ) = assertStatus(client, HttpMethod.Delete, DELETE_URL, token, expectedStatus)

        suspend fun logUserIn(
            client: HttpClient,
            email: String = "john.doe@gmail.com",
            password: String = "JohnDoe123#",
        ): TokenExternalInfoDto =
            client.apiRequest(
                HttpMethod.Post,
                LOGIN_URL,
                expectedStatus = HttpStatusCode.Created,
            ) { setBody(UserCredentialLogin(email, password)) }

        suspend fun logUserInFailure(
            client: HttpClient,
            expectedError: ErrorMessage,
            expectedStatus: HttpStatusCode = HttpStatusCode.BadRequest,
            block: HttpRequestBuilder.() -> Unit = {},
        ) = assertFailure(client, HttpMethod.Post, LOGIN_URL, expectedStatus = expectedStatus, expectedError = expectedError, block = block)

        suspend fun logUserOut(
            client: HttpClient,
            token: String,
        ): Unit = client.apiRequest(HttpMethod.Delete, LOGOUT_URL, token, HttpStatusCode.NoContent)

        suspend fun logUserOutFailure(
            client: HttpClient,
            token: String,
            expectedStatus: HttpStatusCode = HttpStatusCode.Unauthorized,
            expectedError: ErrorMessage? = null,
        ) = if (expectedError != null) {
            assertFailure(client, HttpMethod.Delete, LOGOUT_URL, token, expectedStatus, expectedError)
        } else {
            assertStatus(client, HttpMethod.Delete, LOGOUT_URL, token, expectedStatus)
        }

        suspend fun refresh(
            client: HttpClient,
            token: String,
        ): TokenExternalInfoDto =
            client.apiRequest(
                HttpMethod.Put,
                REFRESH_URL,
                token,
                expectedStatus = HttpStatusCode.OK,
            )

        suspend fun refreshFailure(
            client: HttpClient,
            token: String,
            expectedStatus: HttpStatusCode = HttpStatusCode.Unauthorized,
        ) = assertStatus(client, HttpMethod.Put, REFRESH_URL, token, expectedStatus)
    }
}

private suspend inline fun <reified T> HttpClient.apiRequest(
    method: HttpMethod,
    route: String,
    token: String? = null,
    expectedStatus: HttpStatusCode = HttpStatusCode.OK,
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): T {
    val response =
        request(route) {
            this.method = method
            contentType(ContentType.Application.Json)
            token?.let { bearerAuth(it) }
            block()
        }
    assertEquals(expectedStatus, response.status)
    return response.body()
}
