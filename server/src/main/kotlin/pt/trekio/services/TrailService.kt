package pt.trekio.services

import pt.trekio.domain.Trail
import pt.trekio.errors.DomainError
import pt.trekio.errors.TrailError
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailName
import pt.trekio.misc.TrailType
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.TrailRepository
import pt.trekio.repos.contracts.UserRepository

class TrailService(
    private val trailRepo: TrailRepository,
    private val userRepo: UserRepository,
) : Service() {
    private companion object {
        const val DEFAULT_NAME = "Your Personal Trail"
    }

    fun createTrail(
        token: String,
        start: GeoPoint,
        end: GeoPoint,
        path: List<GeoPoint>,
        name: String = DEFAULT_NAME,
        type: TrailType = TrailType.PRIVATE,
        difficulty: TrailDifficulty = TrailDifficulty.UNKNOWN,
        parent: ULong? = null,
    ): Either<DomainError, ULong> {
        val userId =
            userRepo.getTokenByTokenValidationInfo(token)?.first?.id
                ?: return failure(UserError.InvalidToken)
        var trailName: TrailName

        try {
            trailName = TrailName(name)
        } catch (e: IllegalArgumentException) {
            return failure(TrailError.InvalidTrailName(e.message ?: "Invalid trail name"))
        }

        return trailRepo.createTrail(
            trailName,
            userId,
            start,
            end,
            path,
            type,
            difficulty,
            parent,
        )
    }

    fun getTrail(trailId: ULong): Either<TrailError, Trail> {
        val res = trailRepo.getTrail(trailId) ?: return failure(TrailError.TrailNotFound)
        return success(res)
    }

    fun getTrailsOfUser(
        token: String,
        userId: ULong,
        skip: Int,
        limit: Int,
    ): Either<DomainError, List<Trail>> {
        val ownId =
            userRepo.getTokenByTokenValidationInfo(token)?.first?.id
                ?: return failure(UserError.InvalidToken)

        val isSameUser = userId == ownId
        if (!isSameUser) {
            userRepo.getUserById(userId) ?: return failure(UserError.UserDoesNotExist)
        }

        return paginated(skip, limit) { s, l ->
            trailRepo.getUserTrails(userId, s, l, isSameUser)
        }
    }

    fun getAvailableTrails(
        skip: Int,
        limit: Int,
    ): Either<DomainError, List<Trail>> = paginated(skip, limit, trailRepo::getAvailableTrails)

    fun updateTrail(
        token: String,
        trailId: ULong,
        name: String,
        type: TrailType,
        difficulty: TrailDifficulty,
        parent: ULong?,
    ): Either<DomainError, Unit> {
        val userId =
            userRepo.getTokenByTokenValidationInfo(token)?.first?.id
                ?: return failure(UserError.InvalidToken)
        val trail = trailRepo.getTrail(trailId) ?: return failure(TrailError.TrailNotFound)
        if (trail.creator != userId) {
            return failure(TrailError.TrailNotOwnedByUser(true))
        }
        var trailName: TrailName

        try {
            trailName = TrailName(name)
        } catch (e: IllegalArgumentException) {
            return failure(TrailError.InvalidTrailName(e.message ?: "Invalid trail name"))
        }

        if (parent == trailId) {
            return failure(TrailError.TrailCannotParentItself)
        }

        return trailRepo.editTrail(trailId, trailName, type, difficulty, parent)
    }

    fun removeTrail(
        token: String,
        trailId: ULong,
    ): Either<DomainError, Unit> {
        val userId =
            userRepo.getTokenByTokenValidationInfo(token)?.first?.id
                ?: return failure(UserError.InvalidToken)

        val trail = trailRepo.getTrail(trailId) ?: return failure(TrailError.TrailNotFound)
        if (trail.creator != userId) {
            return failure(TrailError.TrailNotOwnedByUser(false))
        }

        return trailRepo.removeTrail(trailId)
    }
}
