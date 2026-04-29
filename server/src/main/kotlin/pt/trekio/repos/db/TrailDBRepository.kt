package pt.trekio.repos.db

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import pt.trekio.domain.Trail
import pt.trekio.errors.DomainError
import pt.trekio.errors.TrailError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailName
import pt.trekio.misc.TrailType
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
                this[Trails.type],
                this[Trails.difficulty],
                this[Trails.parent]?.value,
            )
    }

    init {
        transaction(Database.connect(conn, DRIVER_NAME, user, password)) {
            if (!Trails.exists()) {
                Trails.ddl.forEach(this::exec)
            }
        }
    }

    override fun createTrail(
        name: TrailName,
        creator: ULong,
        start: GeoPoint,
        end: GeoPoint,
        path: List<GeoPoint>,
        distance: Double,
        type: TrailType,
        difficulty: TrailDifficulty,
        parent: ULong?,
    ): Either<DomainError, ULong> =
        transaction {
            if (parent != null) {
                getTrail(parent) ?: return@transaction failure(TrailError.ParentTrailNotFound)
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
                        it[Trails.type] = type
                        it[Trails.difficulty] = difficulty
                        it[Trails.parent] = parent
                    }.firstOrNull()
                    ?.get(Trails.id)
                    ?: return@transaction failure(DomainError.UnexpectedError)

            success(id.value)
        }

    override fun getTrail(trailId: ULong) =
        transaction {
            Trails
                .selectAll()
                .where(Trails.id eq trailId)
                .firstOrNull()
                ?.toTrail()
        }

    override fun getUserTrails(
        userId: ULong,
        skip: Int,
        limit: Int,
        showPrivateTrails: Boolean,
    ): List<Trail> =
        transaction {
            val filtering =
                if (!showPrivateTrails) {
                    (Trails.creator eq userId) and (Trails.type neq TrailType.PRIVATE)
                } else {
                    Trails.creator eq userId
                }

            Trails
                .selectAll()
                .where(filtering)
                .offset(skip.toLong())
                .limit(limit)
                .toList()
                .map { it.toTrail() }
        }

    override fun getAvailableTrails(
        skip: Int,
        limit: Int,
    ) = transaction {
        Trails
            .selectAll()
            .where(Trails.type neq TrailType.PRIVATE)
            .offset(skip.toLong())
            .limit(limit)
            .toList()
            .map { it.toTrail() }
    }

    override fun editTrail(
        id: ULong,
        name: TrailName,
        type: TrailType,
        difficulty: TrailDifficulty,
        parent: ULong?,
    ): Either<TrailError, Unit> =
        transaction {
            val curr = getTrail(id) ?: return@transaction failure(TrailError.TrailNotFound)

            val parentChanges = curr.parent != parent
            if (parentChanges) {
                if (parent == id) {
                    return@transaction failure(TrailError.TrailCannotParentItself)
                }

                if (parent != null) {
                    getTrail(parent) ?: return@transaction failure(TrailError.ParentTrailNotFound)
                }
            }

            val changes =
                Trails.update({ Trails.id eq id }) {
                    if (curr.name != name) {
                        it[Trails.name] = name.value
                    }

                    if (curr.type != type) {
                        it[Trails.type] = type
                    }

                    if (curr.difficulty != difficulty) {
                        it[Trails.difficulty] = difficulty
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

    override fun deleteTrail(trailId: ULong) =
        transaction {
            val removals = Trails.deleteWhere { Trails.id eq trailId }

            if (removals == 0) {
                failure(TrailError.TrailNotFound)
            } else {
                success(Unit)
            }
        }

    override fun deleteAllTrails(): Unit =
        transaction {
            Trails.deleteAll()
        }
}
