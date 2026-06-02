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
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import pt.trekio.domain.toDto
import pt.trekio.dto.HikeLocationDto
import pt.trekio.dto.ResultIdDto
import pt.trekio.errors.HikeError
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.misc.toGeoPoint
import pt.trekio.redis.RedisResult
import pt.trekio.redis.RedisService
import pt.trekio.server.config.sendError
import pt.trekio.services.HikeService

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

        val geoPointFormatError = """{"error": "Invalid "}  """
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
        webSocketProtectedWithId {
            expectValidId("tid", "trail") { tid ->
                try {
                    val location =
                        call
                            .receive<HikeLocationDto>()
                            .currentLocation
                            .toGeoPoint()

                    val res = service.startHike(it, tid, location)
                    if (res is Failure) {
                        sendError(res.message)
                        return@expectValidId
                    }

                    val redisRes = redisServ.subscribe(tid) { msg ->
                        launch { outgoing.send(Frame.Text(msg)) }
                    }

                    if (redisRes is RedisResult.Failure) {
                        sendError(HikeError.CouldNotStartHike(redisRes.error))
                        return@expectValidId
                    }

                    for (frame in incoming) {
                        when (frame.frameType) {
                            FrameType.PING -> outgoing.send(Frame.Pong(frame.data))

                            FrameType.CLOSE -> {
                                redisServ.unsubscribe(
                                    tid,
                                    (redisRes as RedisResult.Success<ULong>).value
                                )
                                closeNormally()
                            }

                            FrameType.TEXT -> {
                                try {
                                    val point = frame.data.decodeToString().toGeoPoint()
                                } catch (_: Throwable) {
                                    outgoing.send(Frame.Text(parser.encodeToString()))
                                }
                            }

                            else -> continue
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




                call.respond(HttpStatusCode.Created, ResultIdDto((res as Success).value))
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
