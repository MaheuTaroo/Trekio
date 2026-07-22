package pt.trekio.repos.db

import io.lettuce.core.KillArgs.Builder.user
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import pt.trekio.domain.Trail
import pt.trekio.errors.DomainError
import pt.trekio.errors.TrailError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailName
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.TrailRepository
import pt.trekio.repos.db.exposed.Trails

class TrailDBRepository(
    conn: String,
    user: String,
    password: String,
) : TrailRepository {
    private companion object {
        fun ResultRow.toTrail() =
            Trail(
                this[Trails.id].value,
                TrailName(this[Trails.name]),
                this[Trails.creator].value,
                this[Trails.startingPoint],
                this[Trails.endingPoint],
                this[Trails.path],
                this[Trails.distance],
                this[Trails.difficulty],
                this[Trails.parent]?.value,
            )
    }

    init {
        transaction(Database.connect(conn, DRIVER_NAME, user, password)) {
            try {
                exec("SELECT pg_advisory_lock($TRAIL_DB_INIT_LOCK_ID)")
                if (!Trails.exists()) {
                    Trails.ddl.forEach(this::exec)
                    try {
                        exec("CREATE UNIQUE INDEX IF NOT EXISTS idx_trails_name ON trails(lower(trail_name))")
                    } catch (_: Exception) {
                    }
                }
            } finally {
                exec("SELECT pg_advisory_unlock($TRAIL_DB_INIT_LOCK_ID)")
            }
        }
    }

    override suspend fun createTrail(
        name: TrailName,
        creator: ULong,
        start: GeoPoint,
        end: GeoPoint,
        path: List<GeoPoint>,
        distance: Double,
        difficulty: TrailDifficulty,
        parent: ULong?,
    ): Either<DomainError, ULong> =
        suspendTransaction {
            if (parent != null) {
                getTrail(parent) ?: return@suspendTransaction failure(TrailError.ParentTrailNotFound)
            }

            val id =
                Trails
                    .insertReturning(listOf(Trails.id)) {
                        it[Trails.name] = name.value
                        it[Trails.creator] = creator
                        it[Trails.startingPoint] = start
                        it[Trails.endingPoint] = end
                        it[Trails.path] = path
                        it[Trails.distance] = distance
                        it[Trails.difficulty] = difficulty
                        it[Trails.parent] = parent
                    }.firstOrNull()
                    ?.get(Trails.id)
                    ?: return@suspendTransaction failure(DomainError.UnexpectedError)

            success(id.value)
        }

    override suspend fun getTrail(trailId: ULong) =
        suspendTransaction {
            Trails
                .selectAll()
                .where(Trails.id eq trailId)
                .firstOrNull()
                ?.toTrail()
        }

    override suspend fun getUserTrails(
        userId: ULong,
        skip: Int,
        limit: Int,
    ): List<Trail> =
        suspendTransaction {
            Trails
                .selectAll()
                .where(Trails.creator eq userId)
                .offset(skip.toLong())
                .limit(limit)
                .toList()
                .map { it.toTrail() }
        }

    override suspend fun countTrailsOf(userId: ULong) =
        suspendTransaction {
            Trails.selectAll().where { Trails.creator eq userId }.count()
        }

    override suspend fun getAvailableTrails(
        skip: Int,
        limit: Int,
    ) = suspendTransaction {
        Trails
            .selectAll()
            .offset(skip.toLong())
            .limit(limit)
            .toList()
            .map { it.toTrail() }
    }

    override suspend fun editTrail(
        id: ULong,
        name: TrailName,
        parent: ULong?,
    ): Either<TrailError, Unit> =
        suspendTransaction {
            val curr = getTrail(id) ?: return@suspendTransaction failure(TrailError.TrailNotFound)

            val parentChanges = curr.parent != parent
            if (parentChanges) {
                if (parent == id) {
                    return@suspendTransaction failure(TrailError.TrailCannotParentItself)
                }

                if (parent != null) {
                    getTrail(parent) ?: return@suspendTransaction failure(TrailError.ParentTrailNotFound)
                }
            }

            val changes =
                Trails.update({ Trails.id eq id }) {
                    if (curr.name != name) {
                        it[Trails.name] = name.value
                    }

                    if (parentChanges) {
                        it[Trails.parent] = curr.parent
                    }
                }

            if (changes == 0) {
                failure(TrailError.TrailNotFound)
            } else {
                success(Unit)
            }
        }

    override suspend fun deleteTrail(trailId: ULong) =
        suspendTransaction {
            val removals = Trails.deleteWhere { Trails.id eq trailId }

            if (removals == 0) {
                failure(TrailError.TrailNotFound)
            } else {
                success(Unit)
            }
        }

    override suspend fun deleteAllTrails(): Unit =
        suspendTransaction {
            Trails.deleteAll()
        }
}
