package pt.trekio.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveStream
import io.ktor.server.response.respond
import pt.trekio.domain.toDto
import pt.trekio.dto.GeoPointDto
import pt.trekio.dto.ResultIdDto
import pt.trekio.dto.TrailCreate
import pt.trekio.dto.TrailUpdate
import pt.trekio.dto.toDto
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.misc.TrailType
import pt.trekio.misc.toGeoPoint
import pt.trekio.server.config.sendError
import pt.trekio.services.TrailService

class TrailApi(
    private val service: TrailService,
) : Api() {
    fun createTrail(): ClassicControllerMethod =
        classicProtectedWithId {
            val newTrail = call.receive<TrailCreate>()

            val res =
                service.createTrail(
                    it,
                    newTrail.name,
                    newTrail.start.toGeoPoint(),
                    newTrail.end.toGeoPoint(),
                    newTrail.path.map(GeoPointDto::toGeoPoint),
                    if (newTrail.isPrivate) TrailType.PRIVATE else TrailType.PUBLIC,
                    newTrail.firstReview,
                    newTrail.parentId,
                )

            if (res is Failure) {
                call.sendError(res.message)
                return@classicProtectedWithId
            }
            call.respond(
                HttpStatusCode.Created,
                ResultIdDto((res as Success).value),
            )
        }

    fun importTrail(): ClassicControllerMethod =
        classicProtectedWithId {
            val res = service.importTrail(call.receiveStream(), it)

            if (res is Failure) {
                call.sendError(res.message)
                return@classicProtectedWithId
            }

            call.respond(HttpStatusCode.Created, ResultIdDto((res as Success).value))
        }

    fun getTrail(): ClassicControllerMethod =
        classicProtectedWithId {
            expectValidId("tid", "trail") { tid ->
                val res = service.getTrail(tid)
                if (res is Failure) {
                    call.sendError(res.message)
                    return@expectValidId
                }

                call.respond((res as Success).value.toDto())
            }
        }

    fun getTrailsOfUser(): ClassicControllerMethod =
        classicProtectedWithId {
            expectValidId("uid", "user") { uid ->
                paginate { skip, limit ->
                    val res = service.getTrailsOfUser(it, uid, skip, limit)

                    if (res is Failure) {
                        call.sendError(res.message)
                        return@paginate
                    }

                    call.respond((res as Success).value.toDto())
                }
            }
        }

    fun getAvailableTrails(): ClassicControllerMethod =
        classicProtectedWithId {
            paginate { skip, limit ->
                val res = service.getAvailableTrails(skip, limit)

                if (res is Failure) {
                    call.sendError(res.message)
                    return@paginate
                }
                call.respond((res as Success).value.toDto())
            }
        }

    fun updateTrail(): ClassicControllerMethod =
        classicProtectedWithId {
            expectValidId("tid", "trail") { tid ->
                val updateValues = call.receive<TrailUpdate>()

                val res =
                    service.updateTrail(
                        it,
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

    fun removeTrail(): ClassicControllerMethod =
        classicProtectedWithId {
            expectValidId("tid", "trail") { tid ->
                val res =
                    service.removeTrail(
                        it,
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
