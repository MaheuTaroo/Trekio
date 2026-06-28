package pt.trekio.repos.mem

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pt.trekio.domain.Hike
import pt.trekio.domain.Statistics
import pt.trekio.errors.DomainError
import pt.trekio.errors.HikeError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.HikeRepository
import kotlin.time.Instant

typealias HikeOccurrenceKey = Pair<ULong, ULong>

val HikeOccurrenceKey.hikeId
    get() = first
val HikeOccurrenceKey.hikerId
    get() = second

object HikeMemoryRepository : HikeRepository {
    private var hikeId = 1uL
    private val hikes = mutableMapOf<ULong, Hike>()
    private val hikeOccurrences = mutableMapOf<HikeOccurrenceKey, GeoPoint>()
    private val mutex = Mutex()

    override suspend fun startHike(
        trailId: ULong,
        userId: ULong,
        entryPoint: GeoPoint,
        start: Instant,
    ): Either<DomainError, ULong> =
        mutex.withLock {
            hikes[hikeId] =
                Hike(
                    hikeId,
                    userId,
                    trailId,
                    entryPoint,
                    null,
                    start,
                    null,
                )

            hikeOccurrences[hikeId to userId] = entryPoint

            return success(hikeId++)
        }

    override suspend fun getHikeDetails(hikeId: ULong) = mutex.withLock { hikes[hikeId] }

    override suspend fun isCurrentlyHiking(userId: ULong) =
        mutex.withLock {
            hikes.values.any { it.finish == null }
        }

    override suspend fun finishHike(
        hikeId: ULong,
        userId: ULong,
        exitPoint: GeoPoint,
        end: Instant,
    ): Either<DomainError, Unit> =
        mutex.withLock {
            val hike = hikes[hikeId] ?: return@withLock failure(HikeError.HikeNotFound)

            hikes[hikeId] = hike.copy(exit = exitPoint, finish = end)
            hikeOccurrences.remove(hikeId to userId)

            success(Unit)
        }

    override suspend fun deleteHike(hikeId: ULong): Either<DomainError, Unit> =
        mutex.withLock {
            hikes.remove(hikeId) ?: return@withLock failure(HikeError.HikeNotFound)

            success(Unit)
        }

    override suspend fun deleteAllHikes() {
        mutex.withLock {
            hikes.clear()
            hikeOccurrences.clear()
            hikeId = 1uL
        }
    }

    override suspend fun getUserStatistics(userId: ULong): Statistics =
        mutex.withLock {
            val userHikes = hikes.values.filter { it.hiker == userId && it.exit != null }

            if (userHikes.isEmpty()) {
                return@withLock Statistics(userId, 0, 0.0, 0L)
            }

            var totalKm = 0.0
            var totalTime = 0L

            userHikes.forEach {
                totalKm += TrailMemoryRepository.getTrail(it.trail)!!.distance
                totalTime += it.finish!!.toEpochMilliseconds() - it.start.toEpochMilliseconds()
            }

            Statistics(
                userId,
                userHikes.count(),
                totalKm,
                totalTime,
            )
        }
}
