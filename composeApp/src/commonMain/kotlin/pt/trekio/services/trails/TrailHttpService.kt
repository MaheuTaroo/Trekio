package pt.trekio.services.trails

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.path
import pt.trekio.dto.ResultIdDto
import pt.trekio.dto.TrailCreate
import pt.trekio.dto.TrailDto
import pt.trekio.dto.TrailListDto
import pt.trekio.dto.TrailUpdate
import pt.trekio.misc.ApiRoutes
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.toDto
import pt.trekio.repos.UserRepository
import pt.trekio.services.Service

class TrailHttpService(
    userRepo: UserRepository,
    webClient: HttpClient,
) : Service(userRepo, webClient),
    TrailService {
    override suspend fun createTrail(
        name: String,
        start: GeoPoint,
        end: GeoPoint,
        path: List<GeoPoint>,
        difficulty: TrailDifficulty,
        parentId: ULong?,
    ): Either<String, ResultIdDto> =
        generateJsonResponse(ApiRoutes.TrailCreate, { route, token ->
            post {
                url.path(route)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                headers {
                    bearerAuth(token)
                }
                setBody(
                    TrailCreate(
                        name,
                        start.toDto(),
                        end.toDto(),
                        path.map(GeoPoint::toDto),
                        parentId,
                    ),
                )
            }
        }) { }

    override suspend fun importTrail(): Either<String, ResultIdDto> {
        TODO("Missing file system interface")
    }

    override suspend fun getTrailDetails(id: ULong): Either<String, TrailDto> =
        generateJsonResponse(ApiRoutes.TrailById(id), { route, token ->
            post {
                url.path(route)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                headers {
                    bearerAuth(token)
                }
            }
        }) { }

    override suspend fun getTrailsOf(
        userId: ULong,
        page: ULong,
    ): Either<String, TrailListDto> =
        generateJsonResponse(ApiRoutes.UserTrails(userId), { route, token ->
            post {
                url.path(route)
                url.applyPagination(page)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                headers {
                    bearerAuth(token)
                }
            }
        }) { }

    override suspend fun getAllTrails(page: ULong): Either<String, TrailListDto> =
        generateJsonResponse(ApiRoutes.TrailsAvailable, { route, token ->
            post {
                url.path(route)
                accept(ContentType.Application.Json)
                headers {
                    bearerAuth(token)
                }
            }
        }) { }

    override suspend fun updateTrail(
        id: ULong,
        name: String,
        parentId: ULong?,
    ): Either<String, Unit> =
        generateJsonResponse(ApiRoutes.TrailUpdate(id), { route, token ->
            post {
                url.path(route)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                headers {
                    bearerAuth(token)
                }
                setBody(TrailUpdate(name, parentId))
            }
        }) { }

    override suspend fun deleteTrail(id: ULong): Either<String, Unit> =
        generateJsonResponse(ApiRoutes.TrailDelete(id), { route, token ->
            delete {
                url.path(route)
                headers {
                    bearerAuth(token)
                }
            }
        }) { }
}
