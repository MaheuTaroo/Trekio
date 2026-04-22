package pt.trekio.repos.contracts

import pt.trekio.domain.Trail
import pt.trekio.errors.DomainError
import pt.trekio.errors.TrailError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailName
import pt.trekio.misc.TrailType

interface TrailRepository {
    fun createTrail(
        name: TrailName,
        creator: ULong,
        start: GeoPoint,
        end: GeoPoint,
        path: List<GeoPoint>,
        type: TrailType,
        difficulty: TrailDifficulty,
        parent: ULong?,
    ): Either<DomainError, ULong>

    fun getTrail(trailId: ULong): Trail?

    fun getUserTrails(
        userId: ULong,
        skip: Int,
        limit: Int,
        showPrivateTrails: Boolean,
    ): List<Trail>

    fun getAvailableTrails(
        skip: Int,
        limit: Int,
    ): List<Trail>

    fun editTrail(
        id: ULong,
        name: TrailName,
        type: TrailType,
        difficulty: TrailDifficulty,
        parent: ULong?,
    ): Either<TrailError, Unit>

    fun deleteTrail(trailId: ULong): Either<TrailError, Unit>

    fun deleteAllTrails()
}
