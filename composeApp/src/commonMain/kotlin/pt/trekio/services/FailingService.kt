package pt.trekio.services

import pt.trekio.misc.GeoPoint
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailType
import pt.trekio.misc.failure
import pt.trekio.services.hikes.HikeService
import pt.trekio.services.trails.TrailService
import pt.trekio.services.user.UserService

object FailingService : UserService, TrailService, HikeService {
    private const val ERROR = "You are not logged in"

    override suspend fun signUp(
        username: String,
        email: String,
        password: String,
    ) = failure(ERROR)

    override suspend fun login(
        email: String,
        password: String,
    ) = failure(ERROR)

    override suspend fun getOwnDetails() = failure(ERROR)

    override suspend fun getStatsOf(id: ULong) = failure(ERROR)

    override suspend fun deleteUser() = failure(ERROR)

    override suspend fun googlePopup() = failure(ERROR)

    override suspend fun googleCallback() { }

    override suspend fun createTrail(
        name: String,
        start: GeoPoint,
        end: GeoPoint,
        path: List<GeoPoint>,
        difficulty: TrailDifficulty,
        type: TrailType,
        parentId: ULong?,
    ) = failure(ERROR)

    override suspend fun importTrail() = failure(ERROR)

    override suspend fun getTrailDetails(id: ULong) = failure(ERROR)

    override suspend fun startHike(trailId: ULong) = failure(ERROR)

    override suspend fun getHikeDetails(id: ULong) = failure(ERROR)

    override suspend fun finishHike(id: ULong) = failure(ERROR)

    override suspend fun cancelHike(id: ULong) = failure(ERROR)

    override suspend fun getTrailsOf(userId: ULong, page: ULong) = failure(ERROR)

    override suspend fun getAllTrails(page: ULong) = failure(ERROR)

    override suspend fun updateTrail(
        id: ULong,
        name: String,
        type: TrailType,
        difficulty: TrailDifficulty,
        parentId: ULong?,
    ) = failure(ERROR)

    override suspend fun deleteTrail(id: ULong) = failure(ERROR)
}
