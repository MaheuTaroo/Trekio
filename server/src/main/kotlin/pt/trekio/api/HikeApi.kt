package pt.trekio.api

import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.utils.io.CancellationException
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pt.trekio.domain.toDto
import pt.trekio.dto.HikerLocationAndCheckpointDto
import pt.trekio.dto.HikerLocationNoticeDto
import pt.trekio.dto.withoutCheckpoint
import pt.trekio.errors.HikeError
import pt.trekio.errors.toErrorMessage
import pt.trekio.misc.Failure
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.HaversineDistance
import pt.trekio.misc.HaversineDistance.DISTANCE_BETWEEN_POINTS
import pt.trekio.misc.Success
import pt.trekio.misc.toDto
import pt.trekio.misc.toGeoPoint
import pt.trekio.redis.RedisResult
import pt.trekio.redis.RedisService
import pt.trekio.server.config.sendError
import pt.trekio.services.HikeService
import pt.trekio.services.TrailService
import java.util.logging.Logger
import kotlin.collections.mutableListOf
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
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
    private val trailService: TrailService,
    private val hikeService: HikeService,
    private val redis: RedisService,
) : Api() {
    private companion object {
        suspend fun WebSocketServerSession.closeDueToError(msg: String) {
            close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, msg))
        }

        suspend fun WebSocketServerSession.finishSession(dueToCancellation: Boolean = false) {
            close(
                CloseReason(
                    CloseReason.Codes.NORMAL,
                    "Hike has ${if (dueToCancellation) "canceled" else "finished"}",
                ),
            )
        }

        val parser =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

        val logger: Logger = Logger.getLogger(this::class.qualifiedName!!)

        @OptIn(ExperimentalAtomicApi::class)
        val locallyActiveWebSockets = mutableMapOf<ULong, MutableList<HikerSubscriptionData>>()
        val mutex = Mutex()
        val paths = mutableMapOf<ULong, List<GeoPoint>>()
    }

    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun addActiveWebSocket(
        tid: ULong,
        uid: ULong,
        sid: ULong,
    ) {
        mutex.withLock {
            logger.info { "Adding to localActiveWebSocket" }
            locallyActiveWebSockets
                .getOrPut(tid, ::mutableListOf)
                .add(HikerSubscriptionData(uid, sid, AtomicBoolean(false)))
            logger.info { "Local active web socket: $locallyActiveWebSockets" }
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun removeActiveWebSocket(
        tid: ULong,
        sid: ULong,
    ) {
        mutex.withLock {
            locallyActiveWebSockets[tid]?.let { hikes ->
                hikes.removeIf { it.subId == sid }
                if (hikes.isEmpty()) {
                    locallyActiveWebSockets.remove(tid)
                    paths.remove(tid)
                }
            }
        }
    }

    private suspend fun WebSocketServerSession.logAndCloseSession(
        uid: ULong,
        tid: ULong,
        hid: ULong,
        sid: ULong,
        log: String,
    ) {
        removeActiveWebSocket(tid, sid)
        hikeService.cancelHike(uid, hid)
        logger.warning("Hike with uid=$uid/tid=$tid/hid=$hid/sid=$sid closed: $log")
        closeDueToError("hike has been invalidated")
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
        tid: ULong,
        hid: ULong,
        sid: ULong,
    ) {
        val cancelRes = hikeService.cancelHike(uid, hid)

        if (cancelRes is Failure) {
            closeDueToError(cancelRes.message.message)
        } else {
            redis.publish(
                tid,
                sid,
                parser.encodeToString(HikerLocationNoticeDto(uid, null)),
            )
            finishSession(true)
        }
    }

    /**
     * Prepares the needed data for a hiking session through WebSockets.
     *
     * @receiver The WebSockets connection.
     * @param uid The user's identifier.
     * @param tid The trail's identifier.
     * @param isFirstPoint If true and <= 10 meters, hike starts in startPoint,
     * if false and <= 10 meters, hike starts in endPoint.
     * @return The identifiers for the new hike and new Redis subscription.
     */
    private suspend fun WebSocketServerSession.startHikingSession(
        uid: ULong,
        tid: ULong,
        isFirstPoint: Boolean,
    ): Pair<ULong, ULong>? {
        val trail = trailService.getTrail(tid)
        if (trail is Failure) {
            closeDueToError("Trail was not found")
            return null
        }

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

        val hikeIdAndStart = hikeService.startHike(uid, tid, firstLocation, isFirstPoint)
        if (hikeIdAndStart is Failure) {
            closeDueToError(hikeIdAndStart.message.message)
            return null
        }

        val outgoingMutex = Mutex()

        val (id, start) = (hikeIdAndStart as Success).value
        val redisRes =
            redis.subscribe(
                tid,
                parser.encodeToString(
                    HikerLocationAndCheckpointDto(uid, firstLocation.toDto(), start.toDto()),
                ),
                id,
            ) { msg ->
                launch(Dispatchers.IO) {
                    try {
                        val dto = parser.decodeFromString<HikerLocationAndCheckpointDto>(msg)
                        val payload =
                            if (dto.uid == uid) {
                                parser.encodeToString(dto)
                            } else {
                                parser.encodeToString(dto.withoutCheckpoint())
                            }

                        outgoingMutex.withLock {
                            outgoing.send(Frame.Text(payload))
                        }
                    } catch (t: Throwable) {
                        logger.warning { "Failed to relay message to WebSocket: ${t.message}" }
                        t.printStackTrace()
                    }
                }
            }

        if (redisRes is RedisResult.Failure) {
            closeDueToError(HikeError.CouldNotStartHike(redisRes.error).message)
            return null
        }
        val subId = (redisRes as RedisResult.Success<*>).value as ULong
        addActiveWebSocket(tid, uid, subId)

        val successTrail = (trail as Success).value

        mutex.withLock {
            if (paths[tid] == null) {
                paths[tid] = listOf(successTrail.start) + successTrail.path + successTrail.end
            }
        }

        return id to subId
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
    @ExperimentalAtomicApi
    private suspend fun WebSocketServerSession.finishHike(
        uid: ULong,
        tid: ULong,
        hid: ULong,
        sid: ULong,
    ): Int {
        logger.info { "Entering finishHike" }
        val msg = redis.getLatestMessageOfSubscriber(tid, sid)
        if (msg is RedisResult.Failure) {
            // Most likely the hiker is no longer hiking;
            // should get rid of the data, just in case
            redis.unsubscribe(tid, sid)
            removeActiveWebSocket(tid, sid)
            closeDueToError(HikeError.NotCurrentlyHiking.message)
            return 1
        }
        try {
            val (_, endLocation, lastCheckpoint) =
                parser
                    .decodeFromString<HikerLocationAndCheckpointDto>(
                        (msg as RedisResult.Success<*>).value as String,
                    )

            logger.info { "endLocation: $endLocation ; lastCheckpoint: $lastCheckpoint" }

            if (endLocation == null || lastCheckpoint == null) {
                // The hiker may not be hiking anymore once
                // again; must get rid of the data if so
                redis.unsubscribe(tid, sid)
                removeActiveWebSocket(tid, sid)
                closeDueToError(HikeError.NotCurrentlyHiking.message)
                return 1
            }

            val finishRes =
                hikeService.finishHike(
                    uid,
                    hid,
                    endLocation.toGeoPoint(),
                    lastCheckpoint.toGeoPoint(),
                )
            if (finishRes is Failure) {
                redis.unsubscribe(tid, sid)
                removeActiveWebSocket(tid, sid)
                closeDueToError(finishRes.message.message)
                return 2
            }
            redis.publish(
                tid,
                sid,
                parser.encodeToString(HikerLocationAndCheckpointDto(uid, null, null)),
            )

            mutex.withLock {
                val ws = locallyActiveWebSockets[tid]?.firstOrNull { it.subId == sid }
                checkNotNull(ws) {
                    "missing WebSockets session data on finish"
                }
                ws.isClosed.store(true)
            }

            return 0
        } catch (t: Throwable) {
            logger.warning(t::message)
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

    private fun Int.iffPointsAreDifferent(
        first: GeoPoint,
        second: GeoPoint,
    ) = if (first != second) this else 0

    private suspend fun getNextCheckpoint(
        tid: ULong,
        sid: ULong,
        currPoint: GeoPoint,
        startedOnFirstPoint: Boolean,
    ): GeoPoint? {
        val path =
            mutex.withLock {
                paths[tid] ?: throw IllegalStateException("no one is hiking trail $tid")
            }

        logger.info { "Arrived ate getNextCheckpoint" }
        val lastPoint = redis.getLatestMessageOfSubscriber(tid, sid)
        check(lastPoint !is RedisResult.Failure) {
            if (lastPoint is RedisResult.Failure.CouldNotFindMessage) {
                "could not retrieve hiker's last reported location"
            } else {
                (lastPoint as RedisResult.Failure).error
            }
        }

        val lastNotice =
            parser
                .decodeFromString<HikerLocationAndCheckpointDto>(
                    @Suppress("unchecked_cast")
                    (lastPoint as RedisResult.Success<String>).value,
                )

        val lastCheckpoint =
            checkNotNull(lastNotice.lastCheckpoint) {
                "hiker's last saved checkpoint seems to be missing"
            }.toGeoPoint()

        val idxOfLastCheckpoint = path.indexOf(lastCheckpoint)
        check(idxOfLastCheckpoint >= 0) { "hiker's last saved checkpoint is invalid" }

        logger.info { "Last checkpoint index $idxOfLastCheckpoint" }

        val traversingFactor =
            if (startedOnFirstPoint) {
                1.iffPointsAreDifferent(lastCheckpoint, path.last())
            } else {
                (-1).iffPointsAreDifferent(lastCheckpoint, path.first())
            }

        logger.info { "Traversing factor $traversingFactor" }

        val nextCheckpoint = path[idxOfLastCheckpoint + traversingFactor]

        logger.info { "Next checkpoint $nextCheckpoint" }

        return if (HaversineDistance.between(currPoint, nextCheckpoint) <= DISTANCE_BETWEEN_POINTS) {
            nextCheckpoint
        } else {
            null
        }
    }

    /**
     * Attempts to save and publish a user's location to every hiker
     * on the user's trail.
     *
     * @receiver The WebSockets connection.
     * @param uid The user's identifier.
     * @param tid The trail's identifier.
     * @param sid The subscriber's identifier.
     * @param startedOnFirstPoint Whether the hiker started on the first
     * or last point of the trail.
     * @param data The data to process.
     */
    private suspend fun WebSocketServerSession.reportLocation(
        uid: ULong,
        tid: ULong,
        hid: ULong,
        sid: ULong,
        startedOnFirstPoint: Boolean,
        data: String,
    ) {
        try {
            val msg = data.toGeoPoint()

            logger.info { "Arrived at reportLocation" }

            getNextCheckpoint(tid, sid, msg, startedOnFirstPoint)?.let {
                redis.publish(
                    tid,
                    sid,
                    parser.encodeToString(
                        HikerLocationAndCheckpointDto(uid, msg.toDto(), it.toDto()),
                    ),
                )
            }
            /*val distanceToTrail = HaversineDistance.distanceToSegment(msg, prevPoint, nextPoint)

            if (distanceToTrail <= DISTANCE_OFF_TRAIL_THRESHOLD_METERS) {
                redis.publish(
                    tid,
                    sid,
                    parser.encodeToString(
                        HikerLocationAndCheckpointDto(uid, msg, nextCheckpoint),
                    ),
                )
            } else {
                outgoing.send(
                    Frame.Text(
                        parser.encodeToString(
                            ErrorMessage(
                                "You are more than ${DISTANCE_OFF_TRAIL_THRESHOLD_METERS}m away from the next point, please " +
                                    "get closer to it!",
                            ),
                        ),
                    ),
                )
            }*/
        } catch (ise: IllegalStateException) {
            logAndCloseSession(uid, tid, hid, sid, ise.message ?: "unknown illegal state reached")
        } catch (iae: IllegalArgumentException) {
            logAndCloseSession(uid, tid, hid, sid, iae.message ?: "unknown illegal argument received")
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

    /**
     * Indicates whether the WebSockets connection is active or
     * not, checking if the associated hike is still valid. If it
     * has finished or been canceled, the WebSockets connection
     * will be closed and it returns ``false``.
     * @receiver The WebSockets connection.
     * @param tid The trail's identifier.
     * @param sid The subscriber's identifier.
     * @return Whether the session is valid or not.
     */
    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun WebSocketServerSession.isActive(
        tid: ULong,
        sid: ULong,
    ): Boolean {
        val contCode =
            mutex.withLock {
                when {
                    !redis.isActiveSubscription(sid, tid) ->
                        3

                    locallyActiveWebSockets[tid] == null ||
                        locallyActiveWebSockets[tid]!!.none { it.subId == sid } ->
                        2

                    locallyActiveWebSockets[tid]!!.first { it.subId == sid }.isClosed.load() ->
                        1

                    else -> 0
                }
            }

        return when (contCode) {
            1 -> {
                logger.info { "Finish hike successfully" }
                redis.unsubscribe(tid, sid)
                removeActiveWebSocket(tid, sid)
                finishSession()
                false
            }

            2 -> {
                redis.unsubscribe(tid, sid)
                closeDueToError("hike has been invalidated")
                false
            }

            3 -> {
                removeActiveWebSocket(tid, sid)
                redis.unsubscribe(tid, sid)
                closeDueToError("hike is not on redis anymore")
                false
            }

            else -> true
        }
    }

    @ExperimentalAtomicApi
    private suspend fun WebSocketServerSession.handleFrames(
        uid: ULong,
        tid: ULong,
        hid: ULong,
        sid: ULong,
        startedOnFirstPoint: Boolean,
        onFinally: suspend () -> Unit,
    ) {
        try {
            for (frame in incoming) {
                // Supposedly redundant check, but it doesn't hurt :|
                frame as? Frame.Text ?: continue
                logger.info { "Received data from uid=$uid: ${frame.readText()}" }

                when (val data = frame.data.decodeToString()) {
                    "cancel" -> {
                        cancelHike(uid, tid, hid, sid)
                        return
                    }

                    "finish" -> {
                        when (finishHike(uid, tid, hid, sid)) {
                            1 -> {
                                logger.info { "Finish hike failed with 1." }
                                break
                            }

                            2 -> {
                                logger.info { "Finish hike failed with 2." }
                                return
                            }
                            else -> {
                                logger.info { "Finish hike successfully" }
                                val res = hikeService.getHikeDetails(uid, hid)
                                if (res is Failure) {
                                    logger.warning {
                                        "Hike with hid=$hid by user with uid=$uid was completed, but the" +
                                            " hike details could not be fetched: ${res.message}"
                                    }

                                    closeDueToError("hike did finish, but its details couldn't be fetched")
                                } else {
                                    outgoing.send(
                                        Frame.Text(
                                            parser.encodeToString(
                                                (res as Success).value.toDto(),
                                            ),
                                        ),
                                    )

                                    finishSession()
                                    logger.info {
                                        "Successfully marked hike with hid=$hid as finished for user with uid=$uid"
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        reportLocation(uid, tid, hid, sid, startedOnFirstPoint, data)
                    }
                }

                if (!isActive(tid, sid)) {
                    break
                }
            }
        } catch (_: ClosedReceiveChannelException) {
            closeDueToError("Read channel closed")
        } catch (_: ClosedSendChannelException) {
            closeDueToError("Write channel closed")
        } catch (ce: CancellationException) {
            withContext(NonCancellable) {
                logger.info { "WebSocket cancelled: ${ce.message ?: "no message"}" }
                closeDueToError("Channel has been interrupted")
            }
            throw ce
        } catch (t: Throwable) {
            logger.info { "ERROR: WebSocket closed unexpectedly: ${t.message ?: "unknown error"}" }
            t.printStackTrace()
            closeDueToError(t.message ?: "An unknown error occurred")

            /**
             * Just to make sure the hike is deleted after an error;
             * a simple fire-and-forget move since it's just as a last
             * resort
             */
            hikeService.cancelHike(uid, hid)
        } finally {
            withContext(NonCancellable) {
                onFinally()
            }
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun startHike(): WebSocketControllerMethod =
        webSocketProtectedWithId { uid ->
            expectValidId("tid", "trail") { tid ->
                expectParameter("isFirstPoint", "isFirstPoint") { isFirstPoint ->
                    logger.info { "Getting isFirstPoint: $isFirstPoint" }
                    val fp = isFirstPoint.toBooleanStrictOrNull()
                    if (fp == null) {
                        closeDueToError("Parameter isFirstPoint must be a boolean")
                        return@expectParameter
                    }
                    val (hid, sid) = startHikingSession(uid, tid, fp) ?: return@expectParameter

                    handleFrames(uid, tid, hid, sid, fp) {
                        val closeReason = closeReason.await()?.message ?: "abrupt disconnection"
                        logger.info(
                            "Hiker with tid=$tid and sid=$sid closed WebSockets tunnel; " +
                                "reason: ${closeReason.ifBlank { "unknown" }}",
                        )

                        // Just to make sure the data is truly cleared
                        redis.unsubscribe(tid, sid)
                        removeActiveWebSocket(tid, sid)
                    }
                }
            }
        }

    fun getDetails(): ClassicControllerMethod =
        classicProtectedWithId {
            expectValidId("hid", "hike") { hid ->
                val res = hikeService.getHikeDetails(it, hid)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectValidId
                }

                call.respond((res as Success).value.toDto())
            }
        }

    fun getStats(): ClassicControllerMethod =
        classicProtectedWithId {
            expectValidId("uid", "user") { uid ->
                call.respond(hikeService.getUserStatistics(uid).toDto())
            }
        }
}
