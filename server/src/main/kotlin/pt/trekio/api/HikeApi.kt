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
import pt.trekio.misc.toGeoPoint
import pt.trekio.redis.RedisResult
import pt.trekio.redis.RedisService
import pt.trekio.server.config.sendError
import pt.trekio.services.HikeService
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.mutableListOf
import kotlin.concurrent.withLock

class

class HikeApi(
    private val service: HikeService,
    private val redisServ: RedisService,
) : Api() {
    private companion object {
        suspend fun WebSocketServerSession.closeAbnormally(msg: String) {
            close(CloseReason(CloseReason.Codes.CLOSED_ABNORMALLY, msg))
        }

        suspend fun WebSocketServerSession.closeNormally() {
            close(CloseReason(CloseReason.Codes.NORMAL, ""))
        }

        val parser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun DomainError.toWebSocketFrame() =
            Frame.Text(parser.encodeToString(this))
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
                val location =
                    call
                        .receive<HikeLocationDto>()
                        .currentLocation
                        .toGeoPoint()

                val res = service.startHike(uid, tid, location)
                if (res is Failure) {
                    sendError(res.message)
                    return@expectValidId
                }

                val redisRes = redisServ.subscribe(tid, (res as Success).value) { msg ->
                    launch {
                        try {
                            outgoing.send(Frame.Text(msg))
                        } catch (_: Throwable) {

                        }
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
                        when (frame.frameType) {
                            FrameType.PING -> outgoing.send(Frame.Pong(frame.data))

                            FrameType.CLOSE -> {
                                if (!closed) {
                                    redisServ.unsubscribe(tid, subId)
                                    removeActiveWebSocket(tid, subId)
                                    outgoing.send(Frame.Close())
                                }
                                closeNormally()
                            }

                            FrameType.TEXT -> {
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
                            }

                            else -> continue
                        }

                        lock.withLock {
                            if (locallyActiveWebSockets[tid] == null ||
                                locallyActiveWebSockets[tid]!!.none(subId::equals)
                            ) {
                                redisServ.unsubscribe(tid, subId)
                                closed = true
                            } else if (!redisServ.isActiveSubscription(subId, tid)) {
                                removeActiveWebSocket(tid, subId)
                                redisServ.unsubscribe(tid, subId)
                                closed = true
                            }
                        }
                    }
                } catch (_: ClosedReceiveChannelException) {
                    closeAbnormally("Read channel closed")
                } catch (_: ClosedSendChannelException) {
                    closeAbnormally("Write channel closed")
                } catch (_: CancellationException) {
                    closeAbnormally("Thread has been interrupted")
                } catch (t: Throwable) {
                    System.err.println("WebSocket closed unexpectedly: ${t.message ?: "unknown error"}")
                    t.printStackTrace()
                    closeAbnormally(t.message ?: "An unknown error occurred")
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
