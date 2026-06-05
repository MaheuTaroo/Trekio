package pt.trekio.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.utils.io.CancellationException
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.FrameType
import io.ktor.websocket.close
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import pt.trekio.domain.toDto
import pt.trekio.dto.GeoPointDto
import pt.trekio.dto.HikeLocationDto
import pt.trekio.dto.HikerLocationNoticeDto
import pt.trekio.dto.ResultIdDto
import pt.trekio.errors.DomainError
import pt.trekio.errors.HikeError
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
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.seconds

class HikeApi(
    private val service: HikeService,
    private val redisServ: RedisService,
) : Api() {
    private companion object {
        suspend fun WebSocketServerSession.closeDueToError(msg: String) {
            close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, msg))
        }

        suspend fun WebSocketServerSession.closeNormally() {
            close(CloseReason(CloseReason.Codes.NORMAL, ""))
        }

        fun DomainError.toWebSocketFrame() =
            Frame.Text(parser.encodeToString(this))

        val parser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        val logger: Logger = Logger.getLogger(this::class.qualifiedName!!)
    }

    val locallyActiveWebSockets = mutableMapOf<ULong, MutableList<ULong>>()
    val lock = ReentrantLock()

    private fun addActiveWebSocket(tid: ULong, sid: ULong) {
        lock.withLock {
            locallyActiveWebSockets.getOrDefault(tid, mutableListOf()).add(sid)
        }
    }

    private fun removeActiveWebSocket(tid: ULong, sid: ULong) {
        lock.withLock {
            locallyActiveWebSockets[tid]?.let {
                locallyActiveWebSockets[tid]?.let { hikes ->
                    hikes.remove(sid)
                    if (hikes.isEmpty())
                        locallyActiveWebSockets.remove(tid)
                }
            }
        }
    }

    fun startHike(): ClassicControllerMethod =
        classicProtectedWithId {
            expectValidId("tid", "trail") { tid ->
                val location =
                    call
                        .receive<HikeLocationDto>()
                        .currentLocation
                        .toGeoPoint()

                val res = service.startHike(it, tid, location)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectValidId
                }

                call.respond(HttpStatusCode.Created, ResultIdDto((res as Success).value))
            }
        }

    fun startHikeButInWebSockets(): WebSocketControllerMethod =
        webSocketProtectedWithId { uid ->
            expectValidId("tid", "trail") { tid ->
                var closed = false

                val firstLocation = withTimeout(10.seconds) {
                    incoming.receive()
                }

                if (fi)
                val location =
                    call
                        .receive<HikeLocationDto>()
                        .currentLocation
                        .toGeoPoint()

                val res = service.startHike(uid, tid, firstLocation)
                if (res is Failure) {
                    sendError(res.message)
                    return@expectValidId
                }

                val redisRes = redisServ.subscribe(
                    tid,
                    parser.encodeToString(
                        HikerLocationNoticeDto(uid, location.toDto())
                    ),
                    (res as Success).value
                ) { msg ->
                    launch {
                        try {
                            outgoing.send(Frame.Text(msg))
                        } catch (_: Throwable) { }
                    }
                }

                if (redisRes is RedisResult.Failure) {
                    sendError(HikeError.CouldNotStartHike(redisRes.error))
                    return@expectValidId
                }
                val subId = (redisRes as RedisResult.Success<*>).value as ULong
                addActiveWebSocket(tid, subId)

                try {
                    for (frame in incoming) {
                        try {
                            redisServ.publish(
                                tid,
                                subId,
                                parser.encodeToString(
                                    HikerLocationNoticeDto(
                                        uid,
                                        parser.decodeFromString<GeoPointDto>(
                                            frame.data.decodeToString()
                                        )
                                    )
                                ),
                            )
                        } catch (_: Throwable) {
                            outgoing.send(HikeError.IncorrectWebSocketFormat.toWebSocketFrame())
                        }

                        lock.withLock {
                            if (locallyActiveWebSockets[tid] == null ||
                                locallyActiveWebSockets[tid]!!.none(subId::equals)
                            ) {
                                redisServ.unsubscribe(tid, subId)
                                closed = true
                                break
                            } else if (!redisServ.isActiveSubscription(subId, tid)) {
                                removeActiveWebSocket(tid, subId)
                                redisServ.unsubscribe(tid, subId)
                                closed = true
                                break
                            }
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
                        "Hiker with tid=$tid and subId=$subId closed WebSockets tunnel; " +
                        "reason: ${closeReason.ifBlank { "no known reason" }}"
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

                call.respond(HttpStatusCode.Created, (res as Success).value.toDto())
            }
        }

    fun finishHike(): ClassicControllerMethod =
        classicProtectedWithId {
            expectValidId("hid", "hike") { hid ->
                val location =
                    call
                        .receive<HikeLocationDto>()
                        .currentLocation
                        .toGeoPoint()

                val res = service.finishHike(it, hid, location)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectValidId
                }

                call.respond(HttpStatusCode.NoContent, (res as Success).value)
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

                call.respond(HttpStatusCode.Created, (res as Success).value)
            }
        }

    fun getStats(): ClassicControllerMethod =
        classicProtectedWithId {
            expectValidId("uid", "user") { uid ->
                call.respond(service.getUserStatistics(uid).toDto())
            }
        }
}
