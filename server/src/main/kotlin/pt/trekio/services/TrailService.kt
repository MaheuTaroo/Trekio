package pt.trekio.services

import pt.trekio.domain.Trail
import pt.trekio.errors.DomainError
import pt.trekio.errors.TrailError
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailName
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.contracts.TrailRepository
import pt.trekio.repos.contracts.UserRepository
import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import kotlin.math.sqrt

class TrailService(
    private val trailRepo: TrailRepository,
    private val userRepo: UserRepository,
) : GeoService() {
    private companion object {
        private val xmlFactory = XMLInputFactory.newInstance()
        const val DEFAULT_NAME = "Your Personal Trail"
        const val MILES_PER_KM = .621371192
        const val METERS_PER_IN = .3048

        /**
         * Calculates a path's total distance using the Haversine formula,
         * as well as its difficulty.
         *
         * @param start The path's starting point.
         * @param path The intermediate path.
         * @param end The path's ending point.
         * @return The total path in kilometers, and the difficulty based on
         * the United States of America's National Park Service's
         * [difficulty formula](https://www.nps.gov/shen/planyourvisit/how-to-determine-hiking-difficulty.htm).
         *
         * @see [haversineDistance]
         */
        fun calculateDistanceAndDifficulty(
            start: GeoPoint,
            path: List<GeoPoint>,
            end: GeoPoint,
        ): Pair<Double, TrailDifficulty> {
            var nextStart = start
            var currDistance = 0.0
            var elevationGain = 0.0

            path.forEach {
                currDistance += haversineDistance(nextStart, it)
                elevationGain += (it.altitude - nextStart.altitude).coerceAtLeast(0.0)
                nextStart = it
            }

            currDistance += haversineDistance(nextStart, end)

            val distanceInMiles = currDistance * MILES_PER_KM
            val elevGainInFt = elevationGain * METERS_PER_IN
            val npsScore = sqrt(2 * distanceInMiles * elevGainInFt)

            return currDistance to
                when (npsScore) {
                    in Double.NEGATIVE_INFINITY..<100.0 -> TrailDifficulty.BEGINNER
                    in 100.0..<200.0 -> TrailDifficulty.INTERMEDIATE
                    else -> TrailDifficulty.ADVANCED
                }
        }
    }

    fun createTrail(
        userId: ULong,
        name: String,
        start: GeoPoint,
        end: GeoPoint,
        path: List<GeoPoint>,
        parent: ULong? = null,
    ): Either<DomainError, ULong> {
        val user = userRepo.getUserById(userId) ?: return failure(UserError.UserDoesNotExist)
        if (user.rank != UserRank.VERIFIED) {
            return failure(TrailError.UserIsNotVerified)
        }

        var trailName: TrailName

        try {
            trailName = TrailName(name)
        } catch (e: IllegalArgumentException) {
            return failure(TrailError.InvalidTrailName(e.message ?: "Invalid trail name"))
        }

        val (distance, difficulty) = calculateDistanceAndDifficulty(start, path, end)

        return trailRepo.createTrail(
            trailName,
            userId,
            start,
            end,
            path,
            distance,
            difficulty,
            parent,
        )
    }

    fun importTrail(
        stream: InputStream,
        creator: ULong,
    ): Either<DomainError, ULong> {
        val user = userRepo.getUserById(creator) ?: return failure(UserError.UserDoesNotExist)
        if (user.rank != UserRank.VERIFIED) {
            return failure(TrailError.UserIsNotVerified)
        }

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
                                        coords.map { set ->
                                            GeoPoint(set[0], set[1], set[2])
                                        }
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

            val (distance, difficulty) = calculateDistanceAndDifficulty(start, points, end)

            return trailRepo.createTrail(
                realName,
                creator,
                start,
                end,
                points,
                distance,
                difficulty,
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
        userId: ULong,
        skip: Int,
        limit: Int,
    ): Either<DomainError, List<Trail>> {
        userRepo.getUserById(userId) ?: return failure(UserError.UserDoesNotExist)

        return paginated(skip, limit) { s, l ->
            trailRepo.getUserTrails(userId, s, l)
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
        parent: ULong?,
    ): Either<DomainError, Unit> {
        val user = userRepo.getUserById(userId) ?: return failure(UserError.UserDoesNotExist)
        if (user.rank != UserRank.VERIFIED) {
            return failure(TrailError.UserIsNotVerified)
        }

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

        return trailRepo.editTrail(trailId, trailName, parent)
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
