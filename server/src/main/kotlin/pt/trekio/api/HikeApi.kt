package pt.trekio.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import pt.trekio.domain.toDto
import pt.trekio.dto.HikeLocationDto
import pt.trekio.dto.ResultIdDto
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.misc.toGeoPoint
import pt.trekio.redis.RedisService
import pt.trekio.server.config.sendError
import pt.trekio.services.HikeService

class HikeApi(
    private val service: HikeService,
    private val redisServ: RedisService,
) : Api() {
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
