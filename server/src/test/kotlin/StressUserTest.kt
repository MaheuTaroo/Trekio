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
                val totalThreads = 1000
                val groupSize = 5
                val expectedUsers = totalThreads / groupSize // 200

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

                val adminToken = createUser(client).tokenValue
                val users = getUsers(client, adminToken, limit = totalThreads).users
                assertEquals(expectedUsers + 1, users.size)
            }

        @Test
        fun `concurrent user creation with duplicate emails should only create unique users`() =
            testRequests { client ->
                val totalThreads = 1000
                val groupSize = 5
                val expectedUsers = totalThreads / groupSize // 200

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

                val adminToken = createUser(client).tokenValue
                val users = getUsers(client, adminToken, limit = totalThreads).users
                assertEquals(expectedUsers + 1, users.size)
            }
    }

    class GetUsers : BaseTests.Users {
        @Test
        fun `concurrent reads and writes should always return consistent data`() =
            testRequests { client ->
                val totalThreads = 1000
                val adminToken = createUser(client).tokenValue

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
                val finalUsers = getUsers(client, adminToken, limit = 51).users
                assertEquals(51, finalUsers.size)
            }
    }

    class RemoveUser : BaseTests.Users {
        @Test
        fun `concurrent removals should remove each user exactly once`() =
            testRequests { client ->
                val totalUsers = 1000
                val tokens =
                    (0 until totalUsers).map { index ->
                        createUser(client, "User_$index", "user$index@gmail.com", "Password$index#").tokenValue
                    }
                val adminToken = createUser(client).tokenValue

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

    class LogUserIn : BaseTests.Users {
        @Test
        fun `concurrent logins should always invalidate previous token`() =
            testRequests { client ->
                val totalThreads = 1000
                createUser(client)

                val tokens =
                    coroutineScope {
                        (0 until totalThreads)
                            .map {
                                async(Dispatchers.IO) {
                                    runCatchingExpected { logUserIn(client).tokenValue }.getOrNull()
                                }
                            }.awaitAll()
                            .filterNotNull()
                    }

                val validTokens =
                    tokens.count { token ->
                        runCatchingExpected { getSelf(client, token) }.isSuccess
                    }
                assertEquals(1, validTokens)
            }
    }

    class LogUserOut : BaseTests.Users {
        @Test
        fun `concurrent logouts should invalidate each user token exactly once`() =
            testRequests { client ->
                val totalThreads = 1000
                val groupSize = 5

                val tokens =
                    (0 until totalThreads).map { index ->
                        createUser(client, "User_$index", "user$index@gmail.com", "Password$index#").tokenValue
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
