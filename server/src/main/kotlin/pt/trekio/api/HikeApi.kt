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
import pt.trekio.services.HikeService

class HikeApi(
    private val service: HikeService,
) : Api() {
    fun startHike(): ControllerMethod =
        protected {
            expectValidId("tid", "trail") { tid ->
                val location =
                    call
                        .receive<HikeLocationDto>()
                        .currentLocation
                        .toGeoPoint()

                val res = service.startHike(it.token, tid, location)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectValidId
                }

                call.respond(HttpStatusCode.Created, ResultIdDto((res as Success).value))
            }
        }

    fun getDetails(): ControllerMethod =
        protected {
            expectValidId("hid", "hike") { hid ->
                val res = service.getHikeDetails(it.token, hid)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectValidId
                }

                call.respond(HttpStatusCode.Created, (res as Success).value.toDto())
            }
        }

    fun finishHike(): ControllerMethod =
        protected {
            expectValidId("hid", "hike") { hid ->
                val location =
                    call
                        .receive<HikeLocationDto>()
                        .currentLocation
                        .toGeoPoint()

                val res = service.finishHike(it.token, hid, location)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectValidId
                }

                call.respond(HttpStatusCode.NoContent, (res as Success).value)
            }
        }

    fun cancelHike(): ControllerMethod =
        protected {
            expectValidId("hid", "hike") { hid ->
                val res = service.cancelHike(it.token, hid)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectValidId
                }

                call.respond(HttpStatusCode.Created, (res as Success).value)
            }
        }

    fun getStats(): ControllerMethod =
        protected {
            expectParameter("username", "username") { name ->
                val res = service.getUserStatistics(name)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectParameter
                }

                call.respond((res as Success).value.toDto())
            }
        }
}
