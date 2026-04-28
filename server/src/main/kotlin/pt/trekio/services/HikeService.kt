package pt.trekio.services

import pt.trekio.domain.Hike
import pt.trekio.domain.Statistics
import pt.trekio.errors.DomainError
import pt.trekio.errors.HikeError
import pt.trekio.errors.TrailError
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.Username
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
        token: String,
        hid: ULong,
        block: (Hike) -> Either<DomainError, T>,
    ): Either<DomainError, T> {
        val uid =
            userRepo.getTokenByTokenValidationInfo(token)?.first?.id
                ?: return failure(UserError.InvalidToken)

        if (!hikeRepo.isCurrentlyHiking(uid)) {
            return failure(HikeError.NotCurrentlyHiking)
        }

        val details = hikeRepo.getHikeDetails(hid) ?: return failure(HikeError.HikeNotFound)
        if (details.hiker != uid) {
            return failure(HikeError.NotOnTheHike)
        }

        return block(details)
    }

    fun startHike(
        token: String,
        trailId: ULong,
        entryPoint: GeoPoint,
    ): Either<DomainError, ULong> {
        val uid =
            userRepo.getTokenByTokenValidationInfo(token)?.first?.id
                ?: return failure(UserError.InvalidToken)

        if (hikeRepo.isCurrentlyHiking(uid)) {
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

        return hikeRepo.startHike(uid, trailId, trueStart, Clock.System.now())
    }

    fun getHikeDetails(
        token: String,
        hikeId: ULong,
    ): Either<DomainError, Hike> {
        val uid =
            userRepo.getTokenByTokenValidationInfo(token)?.first?.id
                ?: return failure(UserError.InvalidToken)

        val hike = hikeRepo.getHikeDetails(hikeId) ?: return failure(HikeError.HikeNotFound)

        if (hike.hiker != uid) {
            return failure(HikeError.NotOnTheHike)
        }

        return success(hike)
    }

    fun finishHike(
        token: String,
        hikeId: ULong,
        exitPoint: GeoPoint,
    ) = tryEndHike(token, hikeId) {
        val trail =
            trailRepo.getTrail(it.trail)
                ?: return@tryEndHike failure(TrailError.TrailNotFound)
        val trueEnd = if (it.start == trail.start) trail.end else trail.start
        if (haversineDistance(exitPoint, trueEnd) > .01) {
            return@tryEndHike failure(HikeError.InvalidEndingPoint)
        }

        hikeRepo.finishHike(hikeId, exitPoint, Clock.System.now())
    }

    fun cancelHike(
        token: String,
        hikeId: ULong,
    ): Either<DomainError, Unit> =
        tryEndHike(token, hikeId) { _ ->
            hikeRepo.deleteHike(hikeId)
        }

    fun getUserStatistics(username: String): Either<DomainError, Statistics> {
        var name: Username
        try {
            name = Username(username)
        } catch (iae: IllegalArgumentException) {
            return failure(UserError.InvalidUsername(iae.message ?: "Invalid username"))
        }

        val uid =
            userRepo.getUserByName(name)?.id
                ?: return failure(UserError.InvalidToken)

        return success(hikeRepo.getUserStatistics(uid))
    }
}
