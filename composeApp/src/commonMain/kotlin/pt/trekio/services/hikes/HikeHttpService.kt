package pt.trekio.services.hikes

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.headers
import io.ktor.http.path
import pt.trekio.dto.HikeListDto
import pt.trekio.misc.ApiRoutes
import pt.trekio.misc.Either
import pt.trekio.misc.WebSocketCommunicator
import pt.trekio.repos.UserRepository
import pt.trekio.services.Service

class HikeHttpService(
    userRepo: UserRepository,
    webClient: HttpClient,
) : Service(userRepo, webClient),
    HikeService {
    override suspend fun startHike(
        trailId: ULong,
        isFirstPoint: Boolean,
    ): Either<String, WebSocketCommunicator> =
        generateWebSocketStream(ApiRoutes.TrailStart(trailId, isFirstPoint)) { route, token ->
            url.path(route)
            headers {
                bearerAuth(token)
            }
        }

    override suspend fun getMyFinishedHikes(page: ULong): Either<String, HikeListDto> =
        generateJsonResponse(ApiRoutes.HikesBySelf, { route, token ->
            get {
                url.path(route)
                url.applyPagination(page)
                accept(ContentType.Application.Json)
                headers {
                    bearerAuth(token)
                }
            }
        }) { }
}
