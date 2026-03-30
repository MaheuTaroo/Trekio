package pt.trekio

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import pt.trekio.dto.ErrorMessage
import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.dto.UserCreate
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    fun testGetDelete(block: suspend () -> Unit) =
        testApplication {
            application {
                module()
            }

            block()
        }

    fun testPutPost(block: suspend (client: HttpClient) -> Unit) =
        testApplication {
            application {
                module()
            }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

            block(client)
        }

    suspend fun createUser(
        client: HttpClient,
        username: String = "John_Doe",
        email: String = "john.doe@gmail.com",
        password: String = "JohnDoe123#",
    ): TokenExternalInfoDto {
        val response =
            client.post("/users/create") {
                contentType(ContentType.Application.Json)
                setBody(
                    UserCreate(
                        username,
                        email,
                        password,
                    ),
                )
            }
        assertEquals(HttpStatusCode.OK, response.status)
        return response.body()
    }

    suspend fun createUserFailure(
        client: HttpClient,
        expectedError: ErrorMessage,
        expectedStatus: HttpStatusCode = HttpStatusCode.BadRequest,
        block: HttpRequestBuilder.() -> Unit = {},
    ) {
        val response = client.post("/users/create", block)
        assertEquals(expectedStatus, response.status)
        assertEquals(expectedError, response.body())
    }

    @Test
    fun `failed to create a user without body`() =
        testPutPost { client ->
            createUserFailure(
                client,
                ErrorMessage("Request body is missing or malformed; check /docs or /documentation.html on how to use the Trekio API"),
            )
        }

    @Test
    fun `creating a user`() =
        testPutPost { client ->
            createUser(client)
        }

    @Test
    fun `failed to create a user because of name`() =
        testPutPost { client ->
            // createUser(client)
            createUserFailure(client, ErrorMessage("Username already exists"), expectedStatus = HttpStatusCode.Conflict) {
                contentType(ContentType.Application.Json)
                setBody(
                    UserCreate(
                        "John_Doe",
                        "john.doe@gmail.com",
                        "JohnDoe123#",
                    ),
                )
            }

            val invalidUsername =
                listOf(
                    "Jo" to "Username must be between 3 and 32 characters long",
                    "JohnDoe!" to "Username can only have uppercase and lowercase letters, digits, periods and underscores",
                    "1JohnDoe" to "Username must start with a letter",
                )

            invalidUsername.forEachIndexed { index, (username, error) ->
                createUserFailure(client, ErrorMessage(error)) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        UserCreate(
                            username,
                            "john.doe$index@gmail.com",
                            "JohnDoe123#",
                        ),
                    )
                }
            }
        }

    @Test
    fun `failed to create a user because of email`() =
        testPutPost { client ->
            // createUser(client)
            createUserFailure(client, ErrorMessage("Email already in use"), expectedStatus = HttpStatusCode.Conflict) {
                contentType(ContentType.Application.Json)
                setBody(
                    UserCreate(
                        "John",
                        "john.doe@gmail.com",
                        "JohnDoe123#",
                    ),
                )
            }

            val invalidEmails =
                listOf(
                    "john .doe@gmail.com" to "Email should not contain whitespaces",
                    "john   .doe@gmail.com" to "Email should not contain whitespaces",
                    """john
                            .doe@gmail.com""".trim() to "Email should not contain whitespaces",
                    "john..doe@gmail.com" to "Email should not contain 2 consecutive non-alphanumeric characters",
                    "john.@gmail.com" to "Email should not contain 2 consecutive non-alphanumeric characters",
                    "john.doe@.com" to "Email should not contain 2 consecutive non-alphanumeric characters",
                    "john.doe@gmail!.com" to "Email should not contain 2 consecutive non-alphanumeric characters",
                    "@gmail.com" to "Email should start with a uppercase and lowercase letters or a digit",
                    ".john@gmail.com" to "Email should start with a uppercase and lowercase letters or a digit",
                    "john.doe@" to "Email should not end with a special character",
                    "john.doe@gmail." to "Email should not end with a special character",
                    "john.doe@gmail.com!" to "Email should not end with a special character",
                    "john.doe@gmail_com" to "Email should contain at least one period for top-level domain",
                )

            invalidEmails.forEachIndexed { index, (email, error) ->
                createUserFailure(client, ErrorMessage(error)) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        UserCreate(
                            "John$index",
                            email,
                            "JohnDoe123#",
                        ),
                    )
                }
            }
        }

    @Test
    fun `failed to create a user because of password`() {
        testPutPost { client ->
            val invalidPasswords =
                listOf(
                    "        " to "Password should not contain whitespaces",
                    "sudchei " to "Password should not contain whitespaces",
                    "johndoe" to "Password must be at least 8 characters long",
                    "JOHNDOE123#" to "Password must contain at least one lowercase letter",
                    "johndoe123#" to "Password must contain at least one uppercase letter",
                    "Johndoe#" to "Password must contain at least one digit",
                    "JohnDoe123" to "Password must contain at least one symbol",
                )

            invalidPasswords.forEachIndexed { index, (password, error) ->
                createUserFailure(client, ErrorMessage(error)) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        UserCreate(
                            "John$index",
                            "john.doe$index@gmail.com",
                            password,
                        ),
                    )
                }
            }
        }
    }
}
