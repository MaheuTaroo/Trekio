package pt.trekio.services.hikes

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.headers
import io.ktor.http.path
import pt.trekio.dto.HikeDto
import pt.trekio.misc.ApiRoutes
import pt.trekio.misc.Either
import pt.trekio.repos.UserRepository
import pt.trekio.services.Service

class HikeHttpService(
    userRepo: UserRepository,
    webClient: HttpClient,
) : Service(userRepo, webClient),
    HikeService {
    override suspend fun startHike(trailId: ULong): Either<String, Unit> {
        TODO()
    }

    override suspend fun getHikeDetails(id: ULong): Either<String, HikeDto> =
        generateJsonResponse(ApiRoutes.HikeById(id), { route, token ->
            get {
                url.path(route)
                accept(ContentType.Application.Json)
                headers {
                    bearerAuth(token)
                }
            }
        }) { }

    override suspend fun finishHike(id: ULong): Either<String, Unit> =
        generateJsonResponse(ApiRoutes.HikeFinish(id), { route, token ->
            get {
                url.path(route)
                accept(ContentType.Application.Json)
                headers {
                    bearerAuth(token)
                }
            }
        }) { }

    override suspend fun cancelHike(id: ULong): Either<String, Unit> =
        generateJsonResponse(ApiRoutes.HikeCancel(id), { route, token ->
            get {
                url.path(route)
                accept(ContentType.Application.Json)
                headers {
                    bearerAuth(token)
                }
            }
        }) { }
}
