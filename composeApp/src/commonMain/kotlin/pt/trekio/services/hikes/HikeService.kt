package pt.trekio.services.hikes

import pt.trekio.dto.HikeListDto
import pt.trekio.misc.Either
import pt.trekio.misc.WebSocketCommunicator

interface HikeService {
    /**
     * Starts a hike using a WebSockets tunnel.
     * @param trailId The identifier of the trail to hike.
     * @return a WebSockets communication tunnel between
     * the server and the client.
     */
    suspend fun startHike(
        trailId: ULong,
        isFirstPoint: Boolean,
    ): Either<String, WebSocketCommunicator>

    /**
     * Fetches the user's finished hikes.
     * @return the hike details in case of success, or an
     * error message in case of failure.
     */
    suspend fun getMyFinishedHikes(page: ULong = 0uL): Either<String, HikeListDto>
}
