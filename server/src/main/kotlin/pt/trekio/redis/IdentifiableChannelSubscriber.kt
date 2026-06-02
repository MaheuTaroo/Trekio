package pt.trekio.redis

data class IdentifiableChannelSubscriber(
    val id: ULong,
    val client: PubSubClient,
)
