package pt.trekio.services

import pt.trekio.domain.Hike
import pt.trekio.errors.DomainError
import pt.trekio.errors.HikeError
import pt.trekio.errors.TrailError
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Failure
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.UserRank
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.HikeRepository
import pt.trekio.repos.contracts.TrailRepository
import pt.trekio.repos.contracts.UserRepository
import kotlin.time.Clock

class HikeService(
    private val hikeRepo: HikeRepository,
    private val trailRepo: TrailRepository,
    private val userRepo: UserRepository,
) : GeoService() {
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
    ): Either<DomainError, ULong> {
        if (hikeRepo.isCurrentlyHiking(userId)) {
            return failure(HikeError.CurrentlyHiking)
        }

        val trail = trailRepo.getTrail(trailId) ?: return failure(TrailError.TrailNotFound)

        val trueStart: GeoPoint =
            when {
                haversineDistance(trail.start, entryPoint) <= .01 ->
                    trail.start

                haversineDistance(trail.end, entryPoint) <= .01 ->
                    trail.end

                else -> return failure(HikeError.InvalidStartingPoint)
            }

        return hikeRepo.startHike(userId, trailId, trueStart, Clock.System.now())
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
    ) = tryEndHike(userId, hikeId) {
        val trail =
            trailRepo.getTrail(it.trail)
                ?: return@tryEndHike failure(TrailError.TrailNotFound)
        val trueEnd = if (it.start == trail.start) trail.end else trail.start
        if (haversineDistance(exitPoint, trueEnd) > .01) {
            return@tryEndHike failure(HikeError.InvalidEndingPoint)
        }

        val finish = hikeRepo.finishHike(hikeId, userId, exitPoint, Clock.System.now())
        if (finish is Failure) {
            return@tryEndHike finish
        }

        val user = userRepo.getUserById(userId) ?: return@tryEndHike failure(UserError.UserDoesNotExist)
        if (user.rank == UserRank.NEW) {
            val stats = hikeRepo.getUserStatistics(userId)
            if (stats.completedTrails >= 10 || stats.totalKilometersHiked >= 50.0) {
                userRepo.updateUser(user.username, user.copy(rank = UserRank.VERIFIED))
            }
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
