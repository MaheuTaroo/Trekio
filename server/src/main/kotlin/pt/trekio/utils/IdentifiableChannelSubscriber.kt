package pt.trekio.utils

data class IdentifiableChannelSubscriber(
    val id: ULong,
    val client: PubSubClient,
)
