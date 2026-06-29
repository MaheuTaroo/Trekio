import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Enclosed::class)
class StressUserTest {
    class CreateUser : BaseTests.Users {
        @Test
        fun `concurrent user creation with duplicate usernames should only create unique users`() =
            testRequests { client ->
                val totalThreads = 100
                val groupSize = 5
                val expectedUsers = totalThreads / groupSize

                coroutineScope {
                    (0 until totalThreads)
                        .map { index ->
                            async(Dispatchers.IO) {
                                runCatchingExpected {
                                    createUser(
                                        client,
                                        username = "User_${index / groupSize}",
                                        email = "user$index@gmail.com",
                                        password = "Password$index#",
                                    )
                                }
                            }
                        }.awaitAll()
                }

                val adminToken = createUser(client).accessTokenValue
                val users = getUsers(client, adminToken, limit = totalThreads).users
                assertEquals(expectedUsers + 1, users.size)
            }

        @Test
        fun `concurrent user creation with duplicate emails should only create unique users`() =
            testRequests { client ->
                val totalThreads = 100
                val groupSize = 5
                val expectedUsers = totalThreads / groupSize

                coroutineScope {
                    (0 until totalThreads)
                        .map { index ->
                            async(Dispatchers.IO) {
                                runCatchingExpected {
                                    createUser(
                                        client,
                                        username = "User_$index",
                                        email = "user${index / groupSize}@gmail.com",
                                        password = "Password$index#",
                                    )
                                }
                            }
                        }.awaitAll()
                }

                val adminToken = createUser(client).accessTokenValue
                val users = getUsers(client, adminToken, limit = totalThreads).users
                assertEquals(expectedUsers + 1, users.size)
            }
    }

    class GetUsers : BaseTests.Users {
        @Test
        fun `concurrent reads and writes should always return consistent data`() =
            testRequests { client ->
                val totalThreads = 100
                val (adminToken, refreshToken) = createUser(client)

                val readResults =
                    coroutineScope {
                        val writers =
                            (0 until totalThreads).map { index ->
                                async(Dispatchers.IO) {
                                    runCatchingExpected {
                                        createUser(client, "User_$index", "user$index@gmail.com", "Password$index#")
                                    }
                                }
                            }
                        val readers =
                            (0 until totalThreads).map {
                                async(Dispatchers.IO) {
                                    runCatchingExpected {
                                        getUsers(client, adminToken, limit = 51).users.size
                                    }.getOrNull()
                                }
                            }
                        (writers + readers).awaitAll()
                        readers.awaitAll().filterNotNull()
                    }

                assertTrue(readResults.all { it in 1..51 })
                val newAdminToken = refresh(client, refreshToken).accessTokenValue
                val finalUsers = getUsers(client, newAdminToken, limit = 51).users
                assertEquals(51, finalUsers.size)
            }
    }

    class RemoveUser : BaseTests.Users {
        @Test
        fun `concurrent removals should remove each user exactly once`() =
            testRequests { client ->
                val totalUsers = 100
                val tokens =
                    coroutineScope {
                        (0 until totalUsers)
                            .map { index ->
                                async(Dispatchers.Default) {
                                    createUser(client, "User$index", "user$index@gmail.com", "Password$index#").refreshTokenValue
                                }
                            }.awaitAll()
                    }
                val adminToken = createUser(client).accessTokenValue

                coroutineScope {
                    tokens
                        .map { token ->
                            async(Dispatchers.IO) {
                                runCatchingExpected { removeUser(client, token) }
                            }
                        }.awaitAll()
                }

                val remaining = getUsers(client, adminToken, limit = totalUsers + 1).users
                assertEquals(1, remaining.size)
            }
    }

    class Refresh : BaseTests.Users {
        @Test
        fun `concurrent refreshes should always invalidate previous refresh token`() =
            testRequests { client ->
                val totalThreads = 100
                createUser(client)

                val tokens =
                    coroutineScope {
                        (0 until totalThreads)
                            .map {
                                async(Dispatchers.IO) {
                                    runCatchingExpected { logUserIn(client).refreshTokenValue }.getOrNull()
                                }
                            }.awaitAll()
                            .filterNotNull()
                    }

                val validTokens =
                    tokens.count { token ->
                        runCatchingExpected { removeUser(client, token) }.isSuccess
                    }
                assertEquals(1, validTokens)
            }
    }

    class LogUserOut : BaseTests.Users {
        @Test
        fun `concurrent logouts should invalidate each user token exactly once`() =
            testRequests { client ->
                val totalThreads = 100
                val groupSize = 5

                val tokens =
                    coroutineScope {
                        (0 until totalThreads)
                            .map { index ->
                                async(Dispatchers.Default) {
                                    createUser(client, "User$index", "user$index@gmail.com", "Password$index#").refreshTokenValue
                                }
                            }.awaitAll()
                    }

                coroutineScope {
                    tokens
                        .chunked(groupSize)
                        .flatMap { group ->
                            val token = group.first()
                            group.map {
                                async(Dispatchers.IO) {
                                    runCatchingExpected {
                                        logUserOut(client, token)
                                    }
                                }
                            }
                        }.awaitAll()
                }

                tokens.chunked(groupSize).forEach { group ->
                    val token = group.first()
                    getSelfFailure(client, token)
                }
            }
    }
}
