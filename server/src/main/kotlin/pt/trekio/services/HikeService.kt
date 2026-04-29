package pt.trekio.services

import pt.trekio.domain.Hike
import pt.trekio.errors.DomainError
import pt.trekio.errors.HikeError
import pt.trekio.errors.TrailError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
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
    private inline fun <reified T> tryEndHike(
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

    fun startHike(
        userId: ULong,
        trailId: ULong,
        entryPoint: GeoPoint,
    ): Either<DomainError, ULong> {
        if (hikeRepo.isCurrentlyHiking(userId)) {
            return failure(HikeError.CurrentlyHiking)
        }

        val trail = trailRepo.getTrail(trailId) ?: return failure(TrailError.TrailNotFound)

        var trueStart: GeoPoint? = null

        if (haversineDistance(trail.start, entryPoint) <= .01) {
            trueStart = trail.start
        } else if (haversineDistance(trail.end, entryPoint) <= .01) {
            trueStart = trail.end
        }

        if (trueStart == null) {
            return failure(HikeError.InvalidStartingPoint)
        }

        return hikeRepo.startHike(userId, trailId, trueStart, Clock.System.now())
    }

    fun getHikeDetails(
        userId: ULong,
        hikeId: ULong,
    ): Either<DomainError, Hike> {
        val hike = hikeRepo.getHikeDetails(hikeId) ?: return failure(HikeError.HikeNotFound)

        if (hike.hiker != userId) {
            return failure(HikeError.NotOnTheHike)
        }

        return success(hike)
    }

    fun finishHike(
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

        hikeRepo.finishHike(hikeId, userId, exitPoint, Clock.System.now())
    }

    fun cancelHike(
        userId: ULong,
        hikeId: ULong,
    ): Either<DomainError, Unit> =
        tryEndHike(userId, hikeId) { _ ->
            hikeRepo.deleteHike(hikeId)
        }

    fun getUserStatistics(userId: ULong) = hikeRepo.getUserStatistics(userId)
}
