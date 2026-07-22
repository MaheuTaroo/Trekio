package pt.trekio.repos.db

import io.lettuce.core.KillArgs.Builder.user
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import pt.trekio.domain.Hike
import pt.trekio.domain.Statistics
import pt.trekio.errors.DomainError
import pt.trekio.errors.HikeError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.HikeRepository
import pt.trekio.repos.db.exposed.HikeMembers
import pt.trekio.repos.db.exposed.Hikes
import pt.trekio.repos.db.exposed.Trails
import kotlin.time.Instant

class HikeDBRepository(
    conn: String,
    user: String,
    password: String,
) : HikeRepository {
    private companion object {
        fun ResultRow.toHike() =
            Hike(
                this[Hikes.id].value,
                this[Hikes.hiker].value,
                this[Hikes.trail].value,
                this[Hikes.entry],
                this[Hikes.exit],
                Instant.fromEpochMilliseconds(this[Hikes.start]),
                this[Hikes.finish]?.let {
                    Instant.fromEpochMilliseconds(it)
                },
            )
    }

    init {
        transaction(Database.connect(conn, DRIVER_NAME, user, password)) {
            exec("SELECT pg_advisory_lock($HIKE_DB_INIT_LOCK_ID)")
            try {
                val batch = mutableListOf<String>()
                if (!Hikes.exists()) {
                    batch.addAll(Hikes.ddl)
                }
                if (!HikeMembers.exists()) {
                    batch.addAll(HikeMembers.ddl)
                }
                if (batch.isNotEmpty()) {
                    batch.forEach(this::exec)
                    try {
                        exec("CREATE UNIQUE INDEX IF NOT EXISTS idx_hike_members_hiker_id ON hike_members(hiker_id)")
                    } catch (_: Exception) {
                    }
                }
            } finally {
                exec("SELECT pg_advisory_unlock($HIKE_DB_INIT_LOCK_ID)")
            }
        }
    }

    override suspend fun startHike(
        trailId: ULong,
        userId: ULong,
        entryPoint: GeoPoint,
        start: Instant,
    ): Either<DomainError, ULong> =
        suspendTransaction {
            val hikerRes =
                Hikes
                    .insertReturning(listOf(Hikes.id)) {
                        it[Hikes.trail] = trailId
                        it[Hikes.hiker] = userId
                        it[Hikes.entry] = entryPoint
                        it[Hikes.start] = start.toEpochMilliseconds()
                    }.firstOrNull()
                    ?.get(Trails.id)
                    ?: return@suspendTransaction failure(DomainError.UnexpectedError)

            val memberRes =
                HikeMembers
                    .insertReturning(listOf(HikeMembers.hikeId)) {
                        it[HikeMembers.hikeId] = hikerRes
                        it[HikeMembers.hikerId] = userId
                        it[HikeMembers.currentLocation] = entryPoint
                    }.firstOrNull()

            if (memberRes == null) {
                rollback()
                return@suspendTransaction failure(HikeError.CurrentlyHiking)
            }

            success(hikerRes.value)
        }

    override suspend fun getHikeDetails(hikeId: ULong) =
        suspendTransaction {
            Hikes
                .selectAll()
                .where(Hikes.id eq hikeId)
                .firstOrNull()
                ?.toHike()
        }

    override suspend fun isCurrentlyHiking(userId: ULong): Boolean =
        suspendTransaction {
            Hikes
                .select(Hikes.id)
                .where(Hikes.finish eq null)
                .count() != 0L
        }

    override suspend fun finishHike(
        hikeId: ULong,
        userId: ULong,
        exitPoint: GeoPoint,
        end: Instant,
    ): Either<DomainError, Unit> =
        suspendTransaction {
            val res =
                Hikes.update({ Hikes.id eq hikeId }) {
                    it[Hikes.exit] = exitPoint
                    it[Hikes.finish] = end.toEpochMilliseconds()
                }
            if (res == 0) {
                return@suspendTransaction failure(HikeError.HikeNotFound)
            }

            HikeMembers.deleteWhere { HikeMembers.hikeId eq hikeId and (HikeMembers.hikerId eq userId) }
            success(Unit)
        }

    override suspend fun deleteHike(hikeId: ULong): Either<HikeError, Unit> =
        suspendTransaction {
            val removals = Hikes.deleteWhere { Hikes.id eq hikeId }

            if (removals == 0) {
                failure(HikeError.HikeNotFound)
            }

            success(Unit)
        }

    override suspend fun deleteAllHikes(): Unit =
        suspendTransaction {
            Hikes.deleteAll()
            HikeMembers.deleteAll()
        }

    override suspend fun getUserStatistics(userId: ULong): Statistics =
        suspendTransaction {
            val data =
                (Hikes leftJoin Trails)
                    .select(listOf(Hikes.start, Hikes.finish, Trails.distance))
                    .where(Hikes.hiker eq userId and (Hikes.finish neq null))
                    .toList()

            if (data.isEmpty()) {
                return@suspendTransaction Statistics(userId, 0, 0.0, 0L)
            }

            var totalKm = 0.0
            var totalTime = 0L

            data.forEach {
                totalKm += it[Trails.distance]
                totalTime += it[Hikes.finish]!! - it[Hikes.start]
            }

            Statistics(
                userId,
                data.count(),
                totalKm,
                totalTime,
            )
        }
}
