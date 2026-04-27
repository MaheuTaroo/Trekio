package pt.trekio.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveStream
import io.ktor.server.response.respond
import pt.trekio.domain.toDto
import pt.trekio.dto.ResultIdDto
import pt.trekio.dto.TrailCreate
import pt.trekio.dto.TrailPointDto
import pt.trekio.dto.TrailUpdate
import pt.trekio.dto.toDto
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.misc.TrailType
import pt.trekio.misc.toGeoPoint
import pt.trekio.services.TrailService

class TrailApi(
    private val service: TrailService,
) : Api() {
    fun createTrail(): ControllerMethod =
        protected {
            val newTrail = call.receive<TrailCreate>()

            val res =
                service.createTrail(
                    it.token,
                    newTrail.name,
                    newTrail.start.toGeoPoint(),
                    newTrail.end.toGeoPoint(),
                    newTrail.path.map(TrailPointDto::toGeoPoint),
                    if (newTrail.isPrivate) TrailType.PRIVATE else TrailType.IN_TEST,
                    newTrail.firstReview,
                    newTrail.parentId,
                )

            if (res is Failure) {
                call.sendError(res.message)
                return@protected
            }
            call.respond(
                HttpStatusCode.Created,
                ResultIdDto((res as Success).value),
            )
        }

    fun importTrail(): ControllerMethod =
        protected {
            val res = service.importTrail(call.receiveStream(), it.username)

            if (res is Failure) {
                call.sendError(res.message)
                return@protected
            }

            call.respond(HttpStatusCode.Created, ResultIdDto((res as Success).value))
        }

    fun getTrail(): ControllerMethod =
        protected {
            expectValidId("tid", "trail") { tid ->
                val res = service.getTrail(tid)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectValidId
                }

                call.respond((res as Success).value.toDto())
            }
        }

    fun getTrailsOfUser(): ControllerMethod =
        protected {
            expectValidId("uid", "user") { uid ->
                paginate { skip, limit ->
                    val res = service.getTrailsOfUser(it.token, uid, skip, limit)

                    if (res is Failure) {
                        call.sendError(res.message)
                        return@paginate
                    }

                    call.respond((res as Success).value.toDto())
                }
            }
        }

    fun getAvailableTrails(): ControllerMethod =
        protected {
            paginate { skip, limit ->
                val res = service.getAvailableTrails(skip, limit)

                if (res is Failure) {
                    call.sendError(res.message)
                    return@paginate
                }
                call.respond((res as Success).value.toDto())
            }
        }

    fun updateTrail(): ControllerMethod =
        protected {
            expectValidId("tid", "trail") { tid ->
                val updateValues = call.receive<TrailUpdate>()

                val res =
                    service.updateTrail(
                        it.token,
                        tid,
                        updateValues.name,
                        updateValues.type,
                        updateValues.difficulty,
                        updateValues.parent,
                    )

                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectValidId
                }

                call.respond(HttpStatusCode.NoContent, (res as Success).value)
            }
        }

    fun removeTrail(): ControllerMethod =
        protected {
            expectValidId("tid", "trail") { tid ->
                val res =
                    service.removeTrail(
                        it.token,
                        tid,
                    )

                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectValidId
                }

                call.respond(HttpStatusCode.NoContent, (res as Success).value)
            }
        }
}
