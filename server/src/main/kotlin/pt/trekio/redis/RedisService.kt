package pt.trekio.redis

import io.ktor.util.moveToByteArray
import io.ktor.utils.io.core.Closeable
import io.lettuce.core.ClientOptions
import io.lettuce.core.MaintNotificationsConfig
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.serialization.json.Json
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

typealias IdentifiableChannelSubscriber = Pair<ULong, PubSubClient>
val IdentifiableChannelSubscriber.id
    get() = first
val IdentifiableChannelSubscriber.client
    get() = second

class RedisService(private val conn: String): Closeable {
    private companion object {
        val codec =
            object : RedisCodec<ULong, String> {
                private val lock = ReentrantLock()

                override fun decodeKey(bytes: ByteBuffer) =
                    lock.withLock { bytes.getLong(0).toULong() }

                override fun decodeValue(bytes: ByteBuffer) =
                    lock.withLock { bytes.moveToByteArray().decodeToString() }

                override fun encodeKey(key: ULong): ByteBuffer =
                    ByteBuffer.wrap("$key".toByteArray())

                override fun encodeValue(value: String) =
                    ByteBuffer.wrap(value.toByteArray())
            }

        val parser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        val cleaner: Cleaner = Cleaner.create()
    }

    private var closed = false

    private val subscribers = mutableMapOf<ULong, MutableList<IdentifiableChannelSubscriber>>()

    private val lock = ReentrantLock()

    private val cleanable = cleaner.register(this) {
        lock.withLock {
            subscribers.forEach { (channel, subs) ->
                subs.forEach { sub ->
                    unsubscribe(channel, sub.id)
                }
            }
        }
    }

    private fun createClient(): Either<Unit, PubSubClient> {
        val uri = RedisURI.create(URI.create(conn))
        println("URI: $uri")
        val client = RedisClient.create(uri)
        client.options = ClientOptions.builder().maintNotificationsConfig(
            MaintNotificationsConfig.builder().enableMaintNotifications(false).build()
        ).build()
        return try {
            success(client.connectPubSub(codec))
        } catch (_: Throwable) {
            failure(Unit)
        }
    }

    private fun registerSubscription(channelId: ULong, client: PubSubClient) {
        lock.withLock {
            subscribers[channelId] ?: emptyList()
        }
    }

    private inline fun withRedisClient(block: (PubSubClient) -> RedisResult): RedisResult {
        val client = createClient()
        return if (client is Failure)
            RedisResult.Failure.UnreachableServer
        else
            block((client as Success).value)
    }

    fun publish(channelId: ULong, subscriberId: ULong, message: Any) =
        lock.withLock {
            if (closed) return@withLock RedisResult.Failure.ServiceHasClosed

            val channelSubs = subscribers[channelId]
                ?: return@withLock RedisResult.Failure.CouldNotPublish("channel does not exist")

            val sub = channelSubs.firstOrNull { it.id == subscriberId }
                ?: return@withLock RedisResult.Failure.CouldNotPublish("subscriber does not exist")

            try {
                sub.client.sync().publish(channelId, parser.encodeToString(message))
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

    fun subscribe(channelId: ULong, block: (Any) -> Unit) =
        lock.withLock {
            if (closed) return@withLock RedisResult.Failure.ServiceHasClosed

            withRedisClient {
                val adapter = object : RedisPubSubAdapter<ULong, String>() {
                    override fun message(channelId: ULong, message: String) {
                        block(parser.decodeFromString(message))
                    }
                }
                val subId = registerSubscription(channelId, it)
                it.addListener(adapter)
                it.sync().subscribe(channelId)
                RedisResult.Success(subId)
            }
        }

    fun unsubscribe(channelId: ULong, subscriberId: ULong) {
        lock.withLock {
            val channelSubs = subscribers[channelId] ?: return

            if (channelSubs.isEmpty() || (channelSubs.size == 1 && channelSubs.first().id == subscriberId)) {
                subscribers.remove(channelId)
                return
            }

            val sub = channelSubs.firstOrNull { it.id == subscriberId } ?: return
            sub.client.sync().unsubscribe(channelId)
            sub.client.close()

            channelSubs.remove(sub)
        }
    }

    override fun close() {
        if (closed) return

        lock.withLock {
            closed = true

            cleanable.clean()
        }
    }
}