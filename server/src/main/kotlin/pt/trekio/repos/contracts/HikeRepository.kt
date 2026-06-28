package pt.trekio.repos.contracts

import pt.trekio.domain.Hike
import pt.trekio.domain.Statistics
import pt.trekio.errors.DomainError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import kotlin.time.Instant

interface HikeRepository {
    suspend fun startHike(
        trailId: ULong,
        userId: ULong,
        entryPoint: GeoPoint,
        start: Instant,
    ): Either<DomainError, ULong>

    suspend fun getHikeDetails(hikeId: ULong): Hike?

    suspend fun isCurrentlyHiking(userId: ULong): Boolean

    suspend fun finishHike(
        hikeId: ULong,
        userId: ULong,
        exitPoint: GeoPoint,
        end: Instant,
    ): Either<DomainError, Unit>

    suspend fun deleteHike(hikeId: ULong): Either<DomainError, Unit>

    suspend fun deleteAllHikes()

    suspend fun getUserStatistics(userId: ULong): Statistics
}
