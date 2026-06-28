package pt.trekio.repos.contracts

import pt.trekio.domain.Trail
import pt.trekio.errors.DomainError
import pt.trekio.errors.TrailError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailName

interface TrailRepository {
    suspend fun createTrail(
        name: TrailName,
        creator: ULong,
        start: GeoPoint,
        end: GeoPoint,
        path: List<GeoPoint>,
        distance: Double,
        difficulty: TrailDifficulty,
        parent: ULong? = null,
    ): Either<DomainError, ULong>

    suspend fun getTrail(trailId: ULong): Trail?

    suspend fun getUserTrails(
        userId: ULong,
        skip: Int,
        limit: Int,
    ): List<Trail>

    suspend fun getAvailableTrails(
        skip: Int,
        limit: Int,
    ): List<Trail>

    suspend fun editTrail(
        id: ULong,
        name: TrailName,
        parent: ULong?,
    ): Either<TrailError, Unit>

    suspend fun deleteTrail(trailId: ULong): Either<TrailError, Unit>

    suspend fun deleteAllTrails()
}
