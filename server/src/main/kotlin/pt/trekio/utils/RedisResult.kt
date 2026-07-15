package pt.trekio.utils

sealed interface RedisResult {
    data class Success<T>(
        val value: T,
    ) : RedisResult

    sealed class Failure(
        val error: String,
    ) : RedisResult {
        class CouldNotPublish(
            cause: String,
        ) : Failure("Could not publish to channel: $cause")

        data object CouldNotSubscribe : Failure("Could not subscribe to channel")

        data object UnreachableServer : Failure("Redis server is unreachable")

        data object ServiceHasClosed : Failure("Redis service has closed")

        data object CouldNotFindSubscriber : Failure("Could not find subscriber")

        data object CouldNotFindMessage : Failure("Could not fetch latest message")
    }
}
