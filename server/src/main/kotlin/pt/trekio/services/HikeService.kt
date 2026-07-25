package pt.trekio.services

import pt.trekio.domain.Hike
import pt.trekio.errors.DomainError
import pt.trekio.errors.HikeError
import pt.trekio.errors.TrailError
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Failure
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.HaversineDistance
import pt.trekio.misc.HaversineDistance.DISTANCE_BETWEEN_POINTS
import pt.trekio.misc.Success
import pt.trekio.misc.UserRank
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.HikeRepository
import pt.trekio.repos.contracts.TrailRepository
import pt.trekio.repos.contracts.UserRepository
import pt.trekio.repos.db.exposed.HikeMembers.hikeId
import java.util.logging.Logger
import kotlin.time.Clock

class HikeService(
    private val hikeRepo: HikeRepository,
    private val trailRepo: TrailRepository,
    private val userRepo: UserRepository,
) : Service() {
    private companion object {
        val logger: Logger = Logger.getLogger("HikeService")
    }

    private suspend fun processUserAfterHike(userId: ULong): Boolean {
        val user = userRepo.getUserById(userId)
        if (user == null) {
            logger.warning {
                "FREAKISH ERROR NUMBER H.1: " +
                    "could not find user with uid=$userId after successfully ending hike with hid=$hikeId"
            }
            return false
        }

        logger.info { "User with uid=$userId has successfully finished hike with hid=$hikeId" }

        if (user.rank == UserRank.NEW) {
            val stats = hikeRepo.getUserStatistics(userId)
            if (stats.completedTrails >= 10 || stats.totalKilometersHiked >= 50.0) {
                val res = userRepo.updateUser(user.username, user.copy(rank = UserRank.VERIFIED))
                if (res is Failure) {
                    logger.warning {
                        "FREAKISH ERROR NUMBER H.2: could not update user with uid=$userId to verified status"
                    }
                }
                logger.info {
                    "User with uid=$userId is now verified!"
                }
            }
        }

        return true
    }

    private suspend inline fun <reified T> tryEndHike(
        userId: ULong,
        hid: ULong,
        block: (Hike) -> Either<DomainError, T>,
    ): Either<DomainError, T> {
        if (!hikeRepo.isCurrentlyHiking(userId)) {
            return failure(HikeError.NotCurrentlyHiking)
        }

        val details = hikeRepo.getHikeDetails(hid) ?: return failure(HikeError.HikeNotFound)
        if (details.hiker != userId) {
            return failure(HikeError.NotOnTheHike)
        }

        return block(details)
    }

    suspend fun startHike(
        userId: ULong,
        trailId: ULong,
        entryPoint: GeoPoint,
        isFirstPoint: Boolean,
    ): Either<DomainError, Pair<ULong, GeoPoint>> {
        if (hikeRepo.isCurrentlyHiking(userId)) {
            return failure(HikeError.CurrentlyHiking)
        }

        val trail = trailRepo.getTrail(trailId) ?: return failure(TrailError.TrailNotFound)

        val trueStart: GeoPoint =
            when {
                isFirstPoint && HaversineDistance.between(trail.start, entryPoint) <= DISTANCE_BETWEEN_POINTS ->
                    trail.start

                !isFirstPoint && HaversineDistance.between(trail.end, entryPoint) <= DISTANCE_BETWEEN_POINTS ->
                    trail.end

                else -> return failure(HikeError.InvalidStartingPoint)
            }

        val res = hikeRepo.startHike(trailId, userId, trueStart, Clock.System.now())
        if (res is Failure) {
            return res
        }

        return success((res as Success).value to trueStart)
    }

    suspend fun getHikeDetails(
        userId: ULong,
        hikeId: ULong,
    ): Either<DomainError, Hike> {
        val hike = hikeRepo.getHikeDetails(hikeId) ?: return failure(HikeError.HikeNotFound)

        if (hike.hiker != userId) {
            return failure(HikeError.NotOnTheHike)
        }

        return success(hike)
    }

    suspend fun finishHike(
        userId: ULong,
        hikeId: ULong,
        exitPoint: GeoPoint,
        lastCheckPoint: GeoPoint,
    ) = tryEndHike(userId, hikeId) {
        logger.info { "Attempting hike finish for uid=$userId and hid=$hikeId..." }
        val trail = trailRepo.getTrail(it.trail)
        if (trail == null) {
            logger.warning { "Trail ${it.trail} was not found for hid=$hikeId" }
            return@tryEndHike failure(TrailError.TrailNotFound)
        }

        val trueEnd = if (it.start == trail.start) trail.end else trail.start
        if (trueEnd != lastCheckPoint) {
            logger.warning {
                "User with uid=$userId wanted to end hike with hid=$hikeId without checking" +
                    "the ending point $trueEnd, only going to $lastCheckPoint"
            }
            return@tryEndHike failure(HikeError.InvalidEndingPoint)
        }

        val finish = hikeRepo.finishHike(hikeId, userId, exitPoint, Clock.System.now())
        if (finish is Failure) {
            logger.warning {
                "There was an error finish hike with hid=$hikeId for user with uid=$userId: ${finish.message}"
            }
            return@tryEndHike finish
        }

        if (processUserAfterHike(userId)) {
            return@tryEndHike failure(UserError.UserDoesNotExist)
        }

        finish
    }

    suspend fun cancelHike(
        userId: ULong,
        hikeId: ULong,
    ): Either<DomainError, Unit> =
        tryEndHike(userId, hikeId) { _ ->
            hikeRepo.deleteHike(hikeId)
        }

    suspend fun getUserStatistics(userId: ULong) = hikeRepo.getUserStatistics(userId)
}
