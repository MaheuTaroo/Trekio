package pt.trekio.repos.mem

import pt.trekio.domain.Hike
import pt.trekio.domain.Statistics
import pt.trekio.errors.DomainError
import pt.trekio.errors.HikeError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.HikeRepository
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
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
    private val lock = ReentrantLock()

    override fun startHike(
        trailId: ULong,
        userId: ULong,
        entryPoint: GeoPoint,
        start: Instant,
    ): Either<DomainError, ULong> =
        lock.withLock {
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

    override fun getHikeDetails(hikeId: ULong) = lock.withLock { hikes[hikeId] }

    override fun isCurrentlyHiking(userId: ULong) =
        lock.withLock {
            hikes.values.any { it.finish == null }
        }

    override fun finishHike(
        hikeId: ULong,
        userId: ULong,
        exitPoint: GeoPoint,
        end: Instant,
    ): Either<DomainError, Unit> =
        lock.withLock {
            val hike = hikes[hikeId] ?: return@withLock failure(HikeError.HikeNotFound)

            hikes[hikeId] = hike.copy(exit = exitPoint, finish = end)
            hikeOccurrences.remove(hikeId to userId)

            success(Unit)
        }

    override fun deleteHike(hikeId: ULong): Either<DomainError, Unit> =
        lock.withLock {
            hikes.remove(hikeId) ?: return@withLock failure(HikeError.HikeNotFound)

            success(Unit)
        }

    override fun deleteAllHikes() {
        lock.withLock {
            hikes.clear()
            hikeOccurrences.clear()
            hikeId = 1uL
        }
    }

    override fun getUserStatistics(userId: ULong): Statistics =
        lock.withLock {
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
