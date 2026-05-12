import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import pt.trekio.dto.ErrorMessage
import pt.trekio.dto.UserCreate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Enclosed::class)
class UsersTest {
    class CreateUser : BaseTests.Users {
        @Test
        fun `failed to create a user without body`() =
            testRequests { client ->
                createUserFailure(
                    client,
                    ErrorMessage("Incorrect body formation detected; check /docs for the full Trekio API route documentation"),
                    HttpStatusCode.UnsupportedMediaType,
                )
            }

        @Test
        fun `creating a user`() =
            testRequests { client ->
                createUser(client)
            }

        @Test
        fun `failed to create a user because of name`() =
            testRequests { client ->
                createUser(client)
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
            testRequests { client ->
                createUser(client)
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
                                "John_Doe$index",
                                email,
                                "JohnDoe123#",
                            ),
                        )
                    }
                }
            }

        @Test
        fun `failed to create a user because of password`() {
            testRequests { client ->
                val invalidPasswords =
                    listOf(
                        "        " to "Password should not contain whitespaces",
                        "" to "Password must be at least 8 characters long",
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
                                "John_Doe$index",
                                "john.doe$index@gmail.com",
                                password,
                            ),
                        )
                    }
                }
            }
        }
    }

    class GetUsers : BaseTests.Users {
        suspend fun repeatUsers(client: HttpClient): Pair<String, List<String>> {
            val token = createUser(client).accessTokenValue
            val usernames =
                mutableListOf("John_Doe") +
                    (0..20).map {
                        createUser(
                            client,
                            "John_Doe$it",
                            "john.doe$it@gmail.com",
                            "JonhDoe$it#",
                        )
                        "John_Doe$it"
                    }
            return token to usernames
        }

        @Test
        fun `failed to get all users while not authenticated`() {
            testRequests { client ->
                getUsersFailure(client, expectedStatus = HttpStatusCode.Forbidden)
            }
        }

        @Test
        fun `get all the users without skip nor limit`() {
            testRequests { client ->
                val (token, usernames) = repeatUsers(client)
                val names = getUsers(client, token).users.map { user -> user.username }

                assertEquals(10, names.size)
                assertTrue(names.containsAll(usernames.take(10)))
            }
        }

        @Test
        fun `get all the users without skip`() {
            testRequests { client ->
                val (token, usernames) = repeatUsers(client)
                val names = getUsers(client, token, limit = 5).users.map { user -> user.username }

                assertEquals(5, names.size)
                assertTrue(names.containsAll(usernames.take(5)))
            }
        }

        @Test
        fun `get all the users without limit`() {
            testRequests { client ->
                val (token, usernames) = repeatUsers(client)
                val names = getUsers(client, token, skip = 5).users.map { user -> user.username }

                assertEquals(10, names.size)
                assertTrue(names.containsAll(usernames.drop(5).take(10)))
            }
        }

        @Test
        fun `get all the users with skip and limit`() {
            testRequests { client ->
                val (token, usernames) = repeatUsers(client)
                val names = getUsers(client, token, 5, 5).users.map { user -> user.username }

                assertEquals(5, names.size)
                assertTrue(names.containsAll(usernames.drop(5).take(5)))
            }
        }

        @Test
        fun `failed with invalid skip`() {
            testRequests { client ->
                val (token, _) = repeatUsers(client)
                getUsersFailure(client, token, -1, expectedError = ErrorMessage("Skip value must be positive or zero"))
            }
        }

        @Test
        fun `failed with invalid limit`() {
            testRequests { client ->
                val (token, _) = repeatUsers(client)
                getUsersFailure(client, token, limit = 0, expectedError = ErrorMessage("Limit value must be positive"))
            }
        }

        @Test
        fun `failed to get users with refresh token`() {
            testRequests { client ->
                val refreshToken = createUser(client).refreshTokenValue
                getUsersFailure(client, refreshToken, expectedStatus = HttpStatusCode.Forbidden)
            }
        }
    }

    class GetSelf : BaseTests.Users {
        @Test
        fun `get self information`() {
            testRequests { client ->
                val token = createUser(client).accessTokenValue
                assertEquals("John_Doe", getSelf(client, token).username)
            }
        }

        @Test
        fun `failed to get self information, because user does not exist`() {
            testRequests { client ->
                getSelfFailure(client, "")
            }
        }

        @Test
        fun `failed to get self with refresh token`() {
            testRequests { client ->
                val refreshToken = createUser(client).refreshTokenValue
                getSelfFailure(client, refreshToken, expectedStatus = HttpStatusCode.Forbidden)
            }
        }
    }

    class GetUserByName : BaseTests.Users {
        @Test
        fun `get myself by name`() {
            testRequests { client ->
                val token = createUser(client).accessTokenValue
                assertEquals("John_Doe", getUserByName(client, token, "John_Doe").username)
            }
        }

        @Test
        fun `get other user by name`() {
            testRequests { client ->
                createUser(
                    client,
                    "John_Doe1",
                    "john.doe1@gmail.com",
                    "JohnDoe123#",
                ).accessTokenValue
                val token = createUser(client).accessTokenValue
                assertEquals("John_Doe1", getUserByName(client, token, "John_Doe1").username)
            }
        }

        @Test
        fun `failed to get other user by name, because user does not exist`() {
            testRequests { client ->
                val token = createUser(client).accessTokenValue
                getUserByNameFailure(
                    client,
                    token,
                    "John_Doe1",
                    ErrorMessage("User does not exist"),
                    HttpStatusCode.NotFound,
                )
            }
        }

        @Test
        fun `failed to get user by name with refresh token`() {
            testRequests { client ->
                val refreshToken = createUser(client).refreshTokenValue
                getUserByNameFailure(
                    client,
                    refreshToken,
                    "John_Doe",
                    expectedStatus = HttpStatusCode.Forbidden,
                )
            }
        }
    }

    class RemoveUser : BaseTests.Users {
        @Test
        fun `failed to deleted a non-existent user`() {
            testRequests { client ->
                removeUserFailure(
                    client,
                    "",
                )
            }
        }

        @Test
        fun `failed to get users with refresh token`() {
            testRequests { client ->
                val accessTokenValue = createUser(client).accessTokenValue
                removeUserFailure(client, accessTokenValue)
            }
        }

        @Test
        fun `deleted myself`() {
            testRequests { client ->
                val token = createUser(client)
                val accessToken = token.accessTokenValue
                val refreshToken = token.refreshTokenValue
                val token2 =
                    createUser(
                        client,
                        "John_Doe1",
                        "john.doe1@gmail.com",
                        "JohnDoe1234#",
                    ).accessTokenValue
                assertEquals(2, getUsers(client, accessToken).users.size)
                removeUser(client, refreshToken)
                assertEquals(1, getUsers(client, token2).users.size)
            }
        }
    }

    class LogUserIn : BaseTests.Users {
        @Test
        fun `failed to create a user without body`() =
            testRequests { client ->
                logUserInFailure(
                    client,
                    ErrorMessage("Incorrect media type; supported media types: application/json"),
                    HttpStatusCode.UnsupportedMediaType,
                )
            }

        @Test
        fun `login successful and multiple access token possible`() {
            testRequests { client ->
                val token = createUser(client).accessTokenValue
                getSelf(client, token)
                val newToken = logUserIn(client).accessTokenValue
                getSelf(client, newToken)
                getSelf(client, token)
            }
        }
    }

    class LogUserOut : BaseTests.Users {
        @Test
        fun `successfully logs out, but access token still active`() =
            testRequests { client ->
                val token = createUser(client)
                val accessToken = token.accessTokenValue
                val refreshToken = token.refreshTokenValue
                getSelf(client, accessToken)
                logUserOut(client, refreshToken)
                getSelf(client, accessToken)
            }

        @Test
        fun `failed to log out without being logged in`() {
            testRequests { client ->
                val token = createUser(client)
                val accessToken = token.accessTokenValue
                val refreshToken = token.refreshTokenValue
                getSelf(client, accessToken)
                logUserOut(client, refreshToken)
                logUserOutFailure(client, refreshToken)
            }
        }

        @Test
        fun `failed to get users with refresh token`() {
            testRequests { client ->
                val accessToken = createUser(client).accessTokenValue
                logUserOutFailure(client, accessToken)
            }
        }
    }

    class Refresh : BaseTests.Users {
        suspend fun waitForAccessTokenExpiration(
            client: HttpClient,
            accessToken: String,
        ) {
            while (true) {
                val result = runCatchingExpected(HttpStatusCode.Forbidden) { getSelf(client, accessToken) }

                if (result.isFailure) {
                    return
                }

                withContext(Dispatchers.IO) {
                    Thread.sleep(10000)
                }
            }
        }

        @Test
        fun `successfully refresh token`() =
            testRequests { client ->
                val token = createUser(client)
                val accessToken = token.accessTokenValue
                val refreshToken = token.refreshTokenValue
                getSelf(client, accessToken)
                val newToken = refresh(client, refreshToken)
                val newAccessToken = newToken.accessTokenValue
                val newRefreshToken = newToken.refreshTokenValue
                getSelf(client, accessToken)
                getSelf(client, newAccessToken)
                logUserOut(client, newRefreshToken)
                logUserOutFailure(client, newRefreshToken)
            }

        @Test
        fun `successfully refresh after access token expired`() =
            testRequests { client ->
                val token = createUser(client)
                val accessToken = token.accessTokenValue
                val refreshToken = token.refreshTokenValue
                getSelf(client, accessToken)

                waitForAccessTokenExpiration(client, accessToken)

                val newToken = refresh(client, refreshToken)
                val newAccessToken = newToken.accessTokenValue
                val newRefreshToken = newToken.refreshTokenValue

                getSelf(client, newAccessToken)
                logUserOut(client, newRefreshToken)
                logUserOutFailure(client, newRefreshToken)
            }

        @Test
        fun `failed to refresh token without being logged in`() {
            testRequests { client ->
                val token = createUser(client)
                val refreshToken = token.refreshTokenValue
                logUserOut(client, refreshToken)
                refreshFailure(client, refreshToken)
            }
        }

        @Test
        fun `failed to refresh token with access token`() {
            testRequests { client ->
                val accessToken = createUser(client).accessTokenValue
                refreshFailure(client, accessToken)
            }
        }
    }
}
