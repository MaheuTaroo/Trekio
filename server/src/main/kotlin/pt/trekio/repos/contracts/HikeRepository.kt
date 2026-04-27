package pt.trekio.repos.contracts

import pt.trekio.domain.Hike
import pt.trekio.domain.Statistics
import pt.trekio.errors.DomainError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import kotlin.time.Instant

interface HikeRepository {
    fun startHike(
        trailId: ULong,
        userId: ULong,
        entryPoint: GeoPoint,
        start: Instant,
    ): Either<DomainError, ULong>

    fun getHikeDetails(hikeId: ULong): Hike?

    fun isCurrentlyHiking(userId: ULong): Boolean

    fun finishHike(
        hikeId: ULong,
        exitPoint: GeoPoint,
        end: Instant,
    ): Either<DomainError, Unit>

    fun deleteHike(hikeId: ULong): Either<DomainError, Unit>

    fun deleteAllHikes()

    fun getUserStatistics(userId: ULong): Statistics
}
