package pt.trekio.repos.mem

import pt.trekio.domain.Trail
import pt.trekio.errors.TrailError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailName
import pt.trekio.misc.TrailType
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.TrailRepository
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object TrailMemoryRepository : TrailRepository {
    private var nextId = 1uL
    private val trails = mutableMapOf<ULong, Trail>()
    private val lock = ReentrantLock()

    override fun createTrail(
        name: TrailName,
        creator: ULong,
        start: GeoPoint,
        end: GeoPoint,
        path: List<GeoPoint>,
        type: TrailType,
        difficulty: TrailDifficulty,
        parent: ULong?,
    ) = lock.withLock {
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
                type,
                difficulty,
                parent,
            )
        trails[nextId] = trail
        success(nextId++)
    }

    override fun getTrail(trailId: ULong) = trails[trailId]

    override fun getUserTrails(
        userId: ULong,
        skip: Int,
        limit: Int,
        showPrivateTrails: Boolean,
    ): List<Trail> =
        lock.withLock {
            trails.values.filter {
                it.creator == userId &&
                    ((!showPrivateTrails && it.type != TrailType.PRIVATE) || showPrivateTrails)
            }
        }

    override fun getAvailableTrails(
        skip: Int,
        limit: Int,
    ): List<Trail> =
        lock.withLock {
            trails
                .values
                .filterNot { it.type == TrailType.PRIVATE }
                .drop(skip)
                .take(limit)
        }

    override fun editTrail(
        id: ULong,
        name: TrailName,
        type: TrailType,
        difficulty: TrailDifficulty,
        parent: ULong?,
    ): Either<TrailError, Unit> =
        lock.withLock {
            val trail = trails[id]
            if (trails[id] == null) {
                return@withLock failure(TrailError.TrailNotFound)
            }

            trails[id] = trail!!.copy(name = name, type = type, difficulty = difficulty, parent = parent)
            success(Unit)
        }

    override fun deleteTrail(trailId: ULong): Either<TrailError, Unit> =
        lock.withLock {
            trails.remove(trailId) ?: return failure(TrailError.TrailNotFound)
            success(Unit)
        }

    override fun deleteAllTrails() {
        trails.clear()
        nextId = 1uL
    }
}
