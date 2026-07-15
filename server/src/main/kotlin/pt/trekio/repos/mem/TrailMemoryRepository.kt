package pt.trekio.repos.mem

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pt.trekio.domain.Trail
import pt.trekio.errors.TrailError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailName
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.TrailRepository

object TrailMemoryRepository : TrailRepository {
    private var nextId = 1uL
    private val trails = mutableMapOf<ULong, Trail>()
    private val mutex = Mutex()

    override suspend fun createTrail(
        name: TrailName,
        creator: ULong,
        start: GeoPoint,
        end: GeoPoint,
        path: List<GeoPoint>,
        distance: Double,
        difficulty: TrailDifficulty,
        parent: ULong?,
    ) = mutex.withLock {
        if (parent != null) {
            getTrail(parent) ?: return@withLock failure(TrailError.ParentTrailNotFound)
        }

        val trail =
            Trail(
                nextId,
                name,
                creator,
                start,
                end,
                path,
                distance,
                difficulty,
                parent,
            )
        trails[nextId] = trail
        success(nextId++)
    }

    override suspend fun getTrail(trailId: ULong) = mutex.withLock { trails[trailId] }

    override suspend fun getUserTrails(
        userId: ULong,
        skip: Int,
        limit: Int,
    ): List<Trail> =
        mutex.withLock {
            trails.values
                .filter { it.creator == userId }
                .drop(skip)
                .take(limit)
        }

    override suspend fun countTrailsOf(userId: ULong) =
        mutex.withLock {
            trails.values.count { it.creator == userId }.toLong()
        }

    override suspend fun getAvailableTrails(
        skip: Int,
        limit: Int,
    ): List<Trail> =
        mutex.withLock {
            trails
                .values
                .drop(skip)
                .take(limit)
        }

    override suspend fun editTrail(
        id: ULong,
        name: TrailName,
        parent: ULong?,
    ): Either<TrailError, Unit> =
        mutex.withLock {
            val trail = trails[id]
            if (trails[id] == null) {
                return@withLock failure(TrailError.TrailNotFound)
            }

            trails[id] = trail!!.copy(name = name, parent = parent)
            success(Unit)
        }

    override suspend fun deleteTrail(trailId: ULong): Either<TrailError, Unit> =
        mutex.withLock {
            trails.remove(trailId) ?: return failure(TrailError.TrailNotFound)
            success(Unit)
        }

    override suspend fun deleteAllTrails() {
        mutex.withLock {
            trails.clear()
            nextId = 1uL
        }
    }
}
