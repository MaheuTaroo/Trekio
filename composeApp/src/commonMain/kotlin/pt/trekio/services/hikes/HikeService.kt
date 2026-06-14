package pt.trekio.services.hikes

import pt.trekio.dto.HikeDto
import pt.trekio.misc.Either

interface HikeService {
    /**
     * Starts a hike using a WebSockets tunnel.
     * @param trailId The identifier of the trail to hike.
     * @return STILL TO BE DECIDED
     */
    suspend fun startHike(trailId: ULong): Either<String, Unit>

    /**
     * Fetches the details or a current or past hike.
     * @param id The hike identifier.
     * @return the hike details in case of success, or an
     * error message in case of failure.
     */
    suspend fun getHikeDetails(id: ULong): Either<String, HikeDto>

    /**
     * Registers that the user finished a hike.
     * @param id The hike identifier.
     * @return nothing in case of success, or an error
     * message in case of failure.
     */
    suspend fun finishHike(id: ULong): Either<String, Unit>

    /**
     * Registers that the user canceled a hike.
     * @param id The hike identifier.
     * @return nothing in case of success, or an error
     * message in case of failure.
     */
    suspend fun cancelHike(id: ULong): Either<String, Unit>
}
