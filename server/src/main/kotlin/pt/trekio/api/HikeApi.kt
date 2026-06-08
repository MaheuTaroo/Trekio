package pt.trekio.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.utils.io.CancellationException
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pt.trekio.domain.toDto
import pt.trekio.dto.HikeLocationDto
import pt.trekio.dto.HikerLocationNoticeDto
import pt.trekio.errors.HikeError
import pt.trekio.errors.toErrorMessage
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.misc.toDto
import pt.trekio.misc.toGeoPoint
import pt.trekio.redis.RedisResult
import pt.trekio.redis.RedisService
import pt.trekio.server.config.sendError
import pt.trekio.services.HikeService
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import kotlin.collections.mutableListOf
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalAtomicApi::class)
typealias HikerSubscriptionData = Triple<ULong, ULong, AtomicBoolean>

@OptIn(ExperimentalAtomicApi::class)
val HikerSubscriptionData.userId
    get() = first

@OptIn(ExperimentalAtomicApi::class)
val HikerSubscriptionData.subId
    get() = second

@OptIn(ExperimentalAtomicApi::class)
val HikerSubscriptionData.isClosed
    get() = third

class HikeApi(
    private val service: HikeService,
    private val redis: RedisService,
) : Api() {
    private companion object {
        suspend fun WebSocketServerSession.closeDueToError(msg: String) {
            close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, msg))
        }

        suspend fun WebSocketServerSession.finishSession(msg: String = "Hike has finished") {
            close(CloseReason(CloseReason.Codes.NORMAL, msg))
        }

        val parser =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

        val logger: Logger = Logger.getLogger(this::class.qualifiedName!!)
    }

    @OptIn(ExperimentalAtomicApi::class)
    val locallyActiveWebSockets = mutableMapOf<ULong, MutableList<HikerSubscriptionData>>()
    val wsLock = ReentrantLock()
    val hikeLock = ReentrantLock()

    @OptIn(ExperimentalAtomicApi::class)
    private fun addActiveWebSocket(
        tid: ULong,
        uid: ULong,
        sid: ULong,
    ) {
        wsLock.withLock {
            locallyActiveWebSockets
                .getOrDefault(tid, mutableListOf())
                .add(Triple(uid, sid, AtomicBoolean(false)))
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private fun removeActiveWebSocket(
        tid: ULong,
        sid: ULong,
    ) {
        wsLock.withLock {
            locallyActiveWebSockets[tid]?.let {
                locallyActiveWebSockets[tid]?.let { hikes ->
                    hikes.removeIf { it.subId == sid }
                    if (hikes.isEmpty()) {
                        locallyActiveWebSockets.remove(tid)
                    }
                }
            }
        }
    }

    /**
     * Attempts to cancel a hike associated to a WebSockets connection.
     *
     * @receiver The WebSockets connection.
     * @param uid The user's identifier.
     * @param hid The hike's identifier.
     */
    private suspend fun WebSocketServerSession.cancelHike(
        uid: ULong,
        hid: ULong,
    ) {
        val cancelRes = service.cancelHike(uid, hid)

        if (cancelRes is Failure) {
            closeDueToError(cancelRes.message.message)
        } else {
            finishSession("Hike has been canceled")
        }
    }

    /**
     * Prepares the needed data for a hiking session through WebSockets.
     *
     * @receiver The WebSockets connection.
     * @param uid The user's identifier.
     * @param tid The trail's identifier.
     * @return The identifiers for the new hike and new Redis subscription.
     */
    private suspend fun WebSocketServerSession.startHikingSession(
        uid: ULong,
        tid: ULong,
    ): Pair<ULong, ULong>? {
        val firstLocation =
            try {
                withTimeout(10.seconds) {
                    val d = incoming.receive().data.decodeToString()
                    d.toGeoPoint()
                }
            } catch (_: Throwable) {
                closeDueToError("Missing or invalid first location")
                return null
            }

        val hikeId = service.startHike(uid, tid, firstLocation)
        if (hikeId is Failure) {
            closeDueToError(hikeId.message.message)
            return null
        }

        val redisRes =
            redis.subscribe(
                tid,
                parser.encodeToString(
                    HikerLocationNoticeDto(uid, firstLocation.toDto()),
                ),
                (hikeId as Success).value,
            ) { msg ->
                launch {
                    try {
                        outgoing.send(Frame.Text(msg))
                    } catch (_: Throwable) {
                    }
                }
            }

        if (redisRes is RedisResult.Failure) {
            closeDueToError(HikeError.CouldNotStartHike(redisRes.error).message)
            return null
        }
        val subId = (redisRes as RedisResult.Success<*>).value as ULong
        addActiveWebSocket(tid, uid, subId)

        return hikeId.value to subId
    }

    /**
     * Attempts to finish a hike associated to a WebSockets connection.
     *
     * @receiver The WebSockets connection.
     * @param uid The user's identifier.
     * @param tid The trail's identifier.
     * @param hid The hike's identifier.
     * @param sid The subscriber's identifier.
     * @return ``0`` if it finishes correctly, ``1`` if the hiker
     * never reported their location, or ``2`` if an error occurred
     * while finishing the hike.
     */
    private suspend fun WebSocketServerSession.finishHike(
        uid: ULong,
        tid: ULong,
        hid: ULong,
        sid: ULong,
    ): Int {
        val msg = redis.getLatestMessageOfSubscriber(sid, tid)
        if (msg is RedisResult.Failure) {
            // Most likely the hiker is no longer hiking;
            // should get rid of the data, just in case
            redis.unsubscribe(tid, sid)
            removeActiveWebSocket(tid, sid)
            closeDueToError(HikeError.NotCurrentlyHiking.message)
            return 1
        } else {
            try {
                val endLocation =
                    parser
                        .decodeFromString<HikerLocationNoticeDto>(
                            (msg as RedisResult.Success<*>).value as String,
                        ).currentLocation
                        .toGeoPoint()

                val finishRes =
                    service.finishHike(
                        uid,
                        hid,
                        endLocation,
                    )
                if (finishRes is Failure) {
                    redis.unsubscribe(tid, sid)
                    removeActiveWebSocket(tid, sid)
                    closeDueToError(finishRes.message.message)
                    return 2
                }
                finishSession()
                return 0
            } catch (t: Throwable) {
                closeDueToError(
                    if (t is SerializationException || t is IllegalArgumentException) {
                        "could not deserialize last sent message"
                    } else {
                        "unknown hike finishing error"
                    },
                )
                return 2
            }
        }
    }

    private suspend fun WebSocketServerSession.reportLocation(
        uid: ULong,
        tid: ULong,
        sid: ULong,
        frame: Frame,
    ) {
        try {
            val msg =
                frame.data
                    .decodeToString()
                    .toGeoPoint()
                    .toDto()

            redis.publish(
                tid,
                sid,
                parser.encodeToString(
                    HikerLocationNoticeDto(uid, msg),
                ),
            )
        } catch (_: Throwable) {
            outgoing.send(
                Frame.Text(
                    parser.encodeToString(
                        HikeError.IncorrectWebSocketFormat.toErrorMessage(),
                    ),
                ),
            )
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun WebSocketServerSession.shouldStopSession(
        tid: ULong,
        sid: ULong,
    ): Boolean {
        val contCode =
            wsLock.withLock {
                when {
                    !redis.isActiveSubscription(sid, tid) ->
                        3

                    locallyActiveWebSockets[tid] == null ||
                        locallyActiveWebSockets[tid]!!.none { it.subId == sid } ->
                        2

                    !locallyActiveWebSockets[tid]!!.first { it.subId == sid }.isClosed.load() ->
                        1

                    else -> 0
                }
            }

        return when (contCode) {
            1 -> {
                redis.unsubscribe(tid, sid)
                removeActiveWebSocket(tid, sid)
                finishSession()
                true
            }

            2 -> {
                redis.unsubscribe(tid, sid)
                closeDueToError("hike has been invalidated")
                true
            }

            3 -> {
                removeActiveWebSocket(tid, sid)
                redis.unsubscribe(tid, sid)
                closeDueToError("hike has been invalidated")
                true
            }

            else -> false
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun startHike(): WebSocketControllerMethod =
        webSocketProtectedWithId { uid ->
            expectValidId("tid", "trail") { tid ->
                val (hid, sid) = startHikingSession(uid, tid) ?: return@expectValidId

                try {
                    for (frame in incoming) {
                        // Supposedly redundant check, but it doesn't hurt :|
                        frame as? Frame.Text ?: continue

                        when (frame.data.decodeToString()) {
                            "cancel" -> {
                                cancelHike(uid, hid)
                                return@expectValidId
                            }

                            "finish" -> {
                                when (finishHike(uid, tid, hid, sid)) {
                                    1 -> break

                                    2 -> return@expectValidId

                                    else -> { }
                                }
                            }

                            else -> {
                                reportLocation(uid, hid, sid, frame)
                            }
                        }

                        if (shouldStopSession(tid, sid)) {
                            break
                        }
                    }
                } catch (_: ClosedReceiveChannelException) {
                    closeDueToError("Read channel closed")
                } catch (_: ClosedSendChannelException) {
                    closeDueToError("Write channel closed")
                } catch (_: CancellationException) {
                    closeDueToError("Thread has been interrupted")
                } catch (t: Throwable) {
                    System.err.println("WebSocket closed unexpectedly: ${t.message ?: "unknown error"}")
                    t.printStackTrace()
                    closeDueToError(t.message ?: "An unknown error occurred")
                } finally {
                    val closeReason = closeReason.await()?.message ?: "abrupt disconnection"
                    logger.info(
                        "Hiker with tid=$tid and sid=$sid closed WebSockets tunnel; " +
                            "reason: ${closeReason.ifBlank { "no known reason" }}",
                    )
                }
            }
        }

    fun getDetails(): ClassicControllerMethod =
        classicProtectedWithId {
            expectValidId("hid", "hike") { hid ->
                val res = service.getHikeDetails(it, hid)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectValidId
                }

                call.respond((res as Success).value.toDto())
            }
        }

    @OptIn(ExperimentalAtomicApi::class)
    fun finishHike(): ClassicControllerMethod =
        classicProtectedWithId { uid ->
            expectValidId("hid", "hike") { hid ->
                hikeLock.lock()
                val details = service.getHikeDetails(uid, hid)
                if (details is Failure) {
                    hikeLock.unlock()
                    call.sendError(details.message)
                    return@expectValidId
                }

                // Just to force smart cast
                details as Success
                val hikeSub = locallyActiveWebSockets[details.value.trail]?.firstOrNull { it.userId == uid }
                if (hikeSub == null) {
                    service.cancelHike(uid, hid)
                    hikeLock.unlock()
                    call.sendError(HikeError.NotCurrentlyHiking)
                    return@expectValidId
                }

                hikeSub.isClosed.store(true)

                val location =
                    call
                        .receive<HikeLocationDto>()
                        .currentLocation
                        .toGeoPoint()
                val res = service.finishHike(uid, hid, location)
                hikeLock.unlock()

                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectValidId
                }
                call.respond(HttpStatusCode.NoContent, details.value)
            }
        }

    fun cancelHike(): ClassicControllerMethod =
        classicProtectedWithId {
            expectValidId("hid", "hike") { hid ->
                val res = service.cancelHike(it, hid)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectValidId
                }

                call.respond(HttpStatusCode.NoContent, (res as Success).value)
            }
        }

    fun getStats(): ClassicControllerMethod =
        classicProtectedWithId {
            expectValidId("uid", "user") { uid ->
                call.respond(service.getUserStatistics(uid).toDto())
            }
        }
}
