package pt.trekio.services.trails

import pt.trekio.dto.ResultIdDto
import pt.trekio.dto.TrailDto
import pt.trekio.dto.TrailListDto
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.TrailDifficulty

interface TrailService {
    /**
     * Creates a new trail.
     * @param name The trail's name.
     * @param start The trail's starting point.
     * @param end The trail's ending point.
     * @param path The trails intermediate points.
     * @param difficulty The trail's difficulty.
     * @param parentId The trail's parent identifier
     * (null for no parent).
     * @return the new trail's identifier in case of success,
     * or an error message in case of failure.
     */
    suspend fun createTrail(
        name: String,
        start: GeoPoint,
        end: GeoPoint,
        path: List<GeoPoint>,
        difficulty: TrailDifficulty,
        parentId: ULong?,
    ): Either<String, ResultIdDto>

    /**
     * Imports a new trail. WARNING: THIS FUNCTION IS NOT
     * FINAL AS IS!
     * TODO: scout around for multiplatform file choice dialogs
     * @return the new trail's identifier in case of success,
     * or an error message in case of failure.
     */
    suspend fun importTrail(): Either<String, ResultIdDto>

    /**
     * Fetches the trail's details.
     * @param id The trail's identifier.
     * @return the trail's information in case of success, or
     * an error message in case of failure.
     */
    suspend fun getTrailDetails(id: ULong): Either<String, TrailDto>

    /**
     * Fetches the trails created by a user.
     * @param userId The user's identifier.
     * @param page The list's page (starting from 0).
     * @return the list of trails created by the user in case
     * of success, or an error message in case of failure.
     */
    suspend fun getTrailsOf(
        userId: ULong,
        page: ULong = 0uL,
    ): Either<String, TrailListDto>

    /**
     * Fetches a list of public trails.
     * @param page The list's page (starting from 0).
     * @return a list of public trails in case of success, or
     * an error message in case of failure.
     */
    suspend fun getAllTrails(page: ULong = 0uL): Either<String, TrailListDto>

    /**
     * Updates a trail's information.
     * @param id The trail's identifier.
     * @param name The trail's new name.
     * @param parentId The trail's new parent.
     * @return nothing in case of success, or an error message
     * in case of failure.
     */
    suspend fun updateTrail(
        id: ULong,
        name: String,
        parentId: ULong?,
    ): Either<String, Unit>

    /**
     * Removes a trail.
     * @param id The trail's identifier.
     * @return nothing in case of success, or an error message
     * in case of failure.
     */
    suspend fun deleteTrail(id: ULong): Either<String, Unit>
}
