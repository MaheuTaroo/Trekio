package pt.trekio.utils

import io.ktor.util.moveToByteArray
import io.ktor.utils.io.core.Closeable
import io.lettuce.core.ClientOptions
import io.lettuce.core.MaintNotificationsConfig
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import pt.trekio.misc.Either
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.misc.failure
import pt.trekio.misc.success
import java.lang.ref.Cleaner
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

typealias PubSubClient = StatefulRedisPubSubConnection<ULong, String>

class RedisService(
    private val conn: String,
) : Closeable {
    private companion object {
        val codec =
            object : RedisCodec<ULong, String> {
                private val lock = ReentrantLock()

                override fun decodeKey(bytes: ByteBuffer) = lock.withLock { bytes.moveToByteArray().decodeToString().toULong() }

                override fun decodeValue(bytes: ByteBuffer) = lock.withLock { bytes.moveToByteArray().decodeToString() }

                override fun encodeKey(key: ULong): ByteBuffer = ByteBuffer.wrap("$key".toByteArray())

                override fun encodeValue(value: String) = ByteBuffer.wrap(value.toByteArray())
            }

        val cleaner: Cleaner = Cleaner.create()
    }

    private var closed = false

    private val subscribers = mutableMapOf<ULong, MutableList<IdentifiableChannelSubscriber>>()

    private val lock = ReentrantLock()

    private val cleanable =
        cleaner.register(this) {
            lock.withLock {
                subscribers.forEach { (channel, subs) ->
                    subs.forEach { sub ->
                        unsubscribe(channel, sub.id)
                    }
                }
            }
        }

    /**
     * Attempts to create a ready-to-use stateful Redis Pub/Sub connection.
     * @return Either the connection in case of success, or [Unit] in case of connection error.
     */
    private fun createClient(): Either<Unit, PubSubClient> {
        val uri = RedisURI.create(URI.create(conn))
        println("URI: $uri")
        val client = RedisClient.create(uri)
        client.options =
            ClientOptions
                .builder()
                .maintNotificationsConfig(
                    MaintNotificationsConfig.builder().enableMaintNotifications(false).build(),
                ).build()
        return try {
            success(client.connectPubSub(codec))
        } catch (_: Throwable) {
            failure(Unit)
        }
    }

    /**
     * Internally saves a new Redis channel subscriber.
     * @param channelId The channel identifier.
     * @param preferredId A preferred subscriber identifier; defaults to the last subscriber's identifier plus one,
     * or simply one if there were no previous subscribers.
     * @param client The connection associated to the subscriber.
     * @return The generated identifier for the subscriber.
     */
    private fun registerSubscription(
        channelId: ULong,
        preferredId: ULong? = null,
        client: PubSubClient,
    ) = lock.withLock {
        val subs = subscribers.getOrPut(channelId, ::mutableListOf)
        val subId =
            if (preferredId == null || subs.any { it.id == preferredId }) {
                (subs.lastOrNull()?.id ?: 0uL) + 1uL
            } else {
                preferredId
            }
        subs.add(IdentifiableChannelSubscriber(subId, client))

        subId
    }

    /**
     * Inline function to use whenever a new Pub/Sub connection is needed.
     * @param block The operation to perform with the generated connection.
     */
    private inline fun withRedisClient(block: (PubSubClient) -> RedisResult): RedisResult {
        val client = createClient()
        return if (client is Failure) {
            RedisResult.Failure.UnreachableServer
        } else {
            block((client as Success).value)
        }
    }

    /**
     * Publishes a new message inside a channel. May fail if the service has been closed.
     * @param channelId The channel to publish in.
     * @param subscriberId The publishing subscriber.
     * @param message The message to publish.
     * @return Either [Unit] in case of success, or an error message in case of failure.
     */
    fun publish(
        channelId: ULong,
        subscriberId: ULong,
        message: String,
    ) = lock.withLock {
        if (closed) return@withLock RedisResult.Failure.ServiceHasClosed

        val channelSubs =
            subscribers[channelId]
                ?: return@withLock RedisResult.Failure.CouldNotPublish("channel does not exist")

        val sub =
            channelSubs.firstOrNull { it.id == subscriberId }
                ?: return@withLock RedisResult.Failure.CouldNotPublish(
                    "subscriber does not exist or is not on the specified channel",
                )

        try {
            val cmds = sub.client.sync()
            cmds.hset(channelId, subscriberId, message)
            cmds.publish(channelId, message)
            RedisResult.Success(Unit)
        } catch (t: Throwable) {
            val msg = t.message ?: "an unknown publishing error occurred"
            System.err.println("Subscriber $subscriberId of channel $channelId could not publish: $msg")
            t.printStackTrace()
            t.cause?.let {
                System.err.println("Cause: ${t.message ?: "no cause message"}")
                it.printStackTrace()
            }
            RedisResult.Failure.CouldNotPublish(msg)
        }
    }

    /** Subscribes a new client to a channel. May fail if the service has been close.
     * @param channelId The channel to subscribe to.
     * @param block The operation to perform on an incoming message.
     * @return The subscriber ID in case of success, or an error message in case of failure.
     */
    fun subscribe(
        channelId: ULong,
        firstMessage: String,
        preferredId: ULong? = null,
        block: (String) -> Unit,
    ) = lock.withLock {
        if (closed) return@withLock RedisResult.Failure.ServiceHasClosed

        withRedisClient {
            try {
                val adapter =
                    object : RedisPubSubAdapter<ULong, String>() {
                        override fun message(
                            channelId: ULong,
                            message: String,
                        ) {
                            block(message)
                        }
                    }
                val subId = registerSubscription(channelId, preferredId, it)
                it.addListener(adapter)
                val cmds = it.sync()
                cmds.subscribe(channelId)

                // Send all previous messages first, so the new subscriber
                // does not get a duplicate of their own message
                cmds.hgetall(channelId).forEach { (_, msg) ->
                    adapter.message(channelId, msg)
                }
                // Finally register and announce the new message
                cmds.hset(channelId, subId, firstMessage)
                cmds.publish(channelId, firstMessage)
                RedisResult.Success(subId)
            } catch (_: Throwable) {
                RedisResult.Failure.CouldNotSubscribe
            }
        }
    }

    /**
     * Indicates whether there is a subscriber with said identifier in a channel.
     * @param subscriberId The subscriber to search inside the channel.
     * @param channelId The channel to search on.
     * @return ``true`` if said subscriber is inside the specified channel, ``false`` otherwise
     */
    fun isActiveSubscription(
        subscriberId: ULong,
        channelId: ULong,
    ) = lock.withLock {
        createClient().let { client ->
            if (client is Failure) {
                if (subscribers[channelId] != null) {
                    subscribers.remove(channelId)
                }

                return@withLock false
            }

            val cmds = (client as Success).value.sync()
            val isMember = cmds.hexists(channelId, subscriberId)

            if (isMember) {
                val subs = subscribers.getOrPut(channelId, ::mutableListOf)
                if (subs.none { it.id == subscriberId }) {
                    cmds.hdel(channelId, subscriberId)
                    return@withLock false
                }
                return@withLock true
            } else {
                val subs = subscribers[channelId] ?: return@withLock false

                subs.removeAll { it.id == subscriberId }
                return@withLock false
            }
        }
    }

    /**
     * Fetches the last message sent by a subscriber on a channel.
     * @param subscriberId The subscriber who might have sent a message.
     * @param channelId The channel identifier to search by.
     * @return The last message sent by the subscriber, or ``null`` if
     * either the channel or the subscriber doesn't exist.
     */
    fun getLatestMessageOfSubscriber(
        channelId: ULong,
        subscriberId: ULong,
    ): RedisResult =
        withRedisClient {
            val msg =
                it.sync().hget(channelId, subscriberId)
                    ?: return RedisResult.Failure.CouldNotFindMessage

            RedisResult.Success(msg)
        }

    /**
     * Stops the subscriber and unsubscribes it from the channel.
     * @param channelId The channel to unsubscribe from.
     * @param subscriberId The subscriber to stop.
     * @return Either [Unit] on success, or an error message on failure.
     */
    fun unsubscribe(
        channelId: ULong,
        subscriberId: ULong,
    ) = lock.withLock {
        if (closed) return@withLock RedisResult.Failure.ServiceHasClosed

        val channelSubs = subscribers[channelId] ?: return@withLock RedisResult.Failure.CouldNotFindSubscriber

        if (channelSubs.isEmpty()) {
            subscribers.remove(channelId)
            return@withLock RedisResult.Failure.CouldNotFindSubscriber
        }

        val sub =
            channelSubs.firstOrNull { it.id == subscriberId }
                ?: return@withLock RedisResult.Failure.CouldNotFindSubscriber

        val cmds = sub.client.sync()
        cmds.unsubscribe(channelId)
        cmds.hdel(channelId, sub.id)
        sub.client.close()

        channelSubs.remove(sub)
        if (channelSubs.isEmpty()) {
            subscribers.remove(channelId)
        }

        RedisResult.Success(Unit)
    }

    /**
     * Stops all generated subscribers and closes this service, marking it as closed.
     * @see [java.io.Closeable.close]
     */
    override fun close() {
        if (closed) return

        lock.withLock {
            closed = true

            cleanable.clean()
        }
    }
}
