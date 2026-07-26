package pt.trekio.repos.contracts

import pt.trekio.domain.Hike
import pt.trekio.domain.Statistics
import pt.trekio.errors.DomainError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import kotlin.time.Instant

abstract class HikeRepository {
    protected companion object {
        /**
         * Generates a paginated list and appends a flag
         * that indicates whether the data source goes
         * beyond the last element in the list.
         * @param limit The maximum amount of elements of
         * the list to generate.
         * @param listGenerator The lambda expression used
         * to generate the paginated list.
         * @return A pair comprised by the generated list
         * and the generated flag.
         */
        suspend fun generateWithContinuationFlag(
            limit: Int,
            listGenerator: suspend (Int) -> List<Hike>,
        ): Pair<List<Hike>, Boolean> {
            val list = listGenerator(limit + 1)

            val hasMore = list.size == limit + 1

            return (if (hasMore) list.dropLast(1) else list) to hasMore
        }
    }

    abstract suspend fun startHike(
        trailId: ULong,
        userId: ULong,
        entryPoint: GeoPoint,
        start: Instant,
    ): Either<DomainError, ULong>

    abstract suspend fun getHikeDetails(hikeId: ULong): Hike?

    abstract suspend fun getFinishedHikesOf(
        userId: ULong,
        skip: Int,
        limit: Int,
    ): Pair<List<Hike>, Boolean>

    abstract suspend fun isCurrentlyHiking(userId: ULong): Boolean

    abstract suspend fun finishHike(
        hikeId: ULong,
        userId: ULong,
        exitPoint: GeoPoint,
        end: Instant,
    ): Either<DomainError, Unit>

    abstract suspend fun deleteHike(hikeId: ULong): Either<DomainError, Unit>

    abstract suspend fun deleteAllHikes()

    abstract suspend fun getUserStatistics(userId: ULong): Statistics
}
