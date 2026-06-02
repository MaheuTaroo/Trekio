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
import pt.trekio.misc.UserRank
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.TrailRepository
import pt.trekio.repos.contracts.UserRepository
import java.io.InputStream
import javax.xml.stream.XMLInputFactory

class TrailService(
    private val trailRepo: TrailRepository,
    private val userRepo: UserRepository,
) : GeoService() {
    private companion object {
        private val xmlFactory = XMLInputFactory.newInstance()
        const val DEFAULT_NAME = "Your Personal Trail"
    }

    /** Calculates a path's total distance using the Haversine formula.
     *
     * @param start The path's starting point.
     * @param path The intermediate path.
     * @param end The path's ending point.
     * @return The total path, in kilometers.
     *
     * @see haversineDistance
     */
    private fun calculateDistance(
        start: GeoPoint,
        path: List<GeoPoint>,
        end: GeoPoint,
    ): Double {
        var nextStart = start
        var currDistance = 0.0

        path.forEach {
            currDistance += haversineDistance(nextStart, it)
            nextStart = it
        }

        currDistance += haversineDistance(nextStart, end)

        return currDistance
    }

    fun createTrail(
        userId: ULong,
        name: String,
        start: GeoPoint,
        end: GeoPoint,
        path: List<GeoPoint>,
        type: TrailType,
        difficulty: TrailDifficulty,
        parent: ULong? = null,
    ): Either<DomainError, ULong> {
        val user = userRepo.getUserById(userId) ?: return failure(UserError.UserDoesNotExist)
        if (user.rank != UserRank.VERIFIED)
            return failure(TrailError.UserIsNotVerified)

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
            calculateDistance(start, path, end),
            type,
            difficulty,
            parent,
        )
    }

    fun importTrail(
        userId: ULong,
        stream: InputStream,
    ): Either<DomainError, ULong> {
        val user = userRepo.getUserById(userId) ?: return failure(UserError.UserDoesNotExist)
        if (user.rank != UserRank.VERIFIED)
            return failure(TrailError.UserIsNotVerified)

        try {
            val reader = xmlFactory.createXMLEventReader(stream)
            var name = DEFAULT_NAME
            val points = mutableListOf<GeoPoint>()

            while (reader.hasNext()) {
                val event = reader.nextEvent()
                if (event.isStartElement) {
                    when (event.asStartElement().name.localPart) {
                        "name" -> {
                            name = reader.elementText
                        }

                        "coordinates" -> {
                            points.addAll(
                                reader
                                    .elementText
                                    .split("\n", " ", "\t")
                                    .filter(String::isNotBlank)
                                    .flatMap {
                                        val splitAndTrimmed = it.split(",", "\n")
                                        val coords =
                                            splitAndTrimmed
                                                .map { str ->
                                                    str.filterNot(Char::isWhitespace).toDouble()
                                                }.chunked(3)
                                        coords.map { set -> GeoPoint(set[0], set[1], set[2]) }
                                    },
                            )
                        }

                        else -> continue
                    }
                }
            }
            val realName: TrailName
            try {
                realName = TrailName(name)
            } catch (iae: IllegalArgumentException) {
                return failure(TrailError.InvalidTrailName(iae.message ?: "Invalid trail name"))
            }

            if (points.size < 3) {
                return failure(TrailError.TrailTooShort)
            }

            val start = points.removeFirst()
            val end = points.removeLast()

            return trailRepo.createTrail(
                realName,
                userId,
                start,
                end,
                points,
                calculateDistance(start, points, end),
            )
        } catch (t: Throwable) {
            println(t.message ?: "<error on kml>")
            return failure(TrailError.WrongTrailFormat)
        }
    }

    fun getTrail(trailId: ULong): Either<TrailError, Trail> {
        val res = trailRepo.getTrail(trailId) ?: return failure(TrailError.TrailNotFound)
        return success(res)
    }

    fun getTrailsOfUser(
        ownId: ULong,
        userId: ULong,
        skip: Int,
        limit: Int,
    ): Either<DomainError, List<Trail>> {
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
        userId: ULong,
        trailId: ULong,
        name: String,
        type: TrailType,
        difficulty: TrailDifficulty,
        parent: ULong?,
    ): Either<DomainError, Unit> {
        val user = userRepo.getUserById(userId) ?: return failure(UserError.UserDoesNotExist)
        if (user.rank != UserRank.VERIFIED)
            return failure(TrailError.UserIsNotVerified)

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
        userId: ULong,
        trailId: ULong,
    ): Either<DomainError, Unit> {
        val trail = trailRepo.getTrail(trailId) ?: return failure(TrailError.TrailNotFound)
        if (trail.creator != userId) {
            return failure(TrailError.TrailNotOwnedByUser(false))
        }

        return trailRepo.deleteTrail(trailId)
    }
}
