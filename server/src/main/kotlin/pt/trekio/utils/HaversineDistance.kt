package pt.trekio.utils

import pt.trekio.misc.GeoPoint
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object HaversineDistance {
    private const val MEAN_SPHERICAL_EARTH_RADIUS = 6371.0088

    /**
     * Calculates the distance between two points, following the
     * Haversine formula. Does not use the point's altitude.
     *
     * Reference: [Haversine formula](https://en.wikipedia.org/wiki/Haversine_formula#Formulation)
     *
     * @param first The starting point.
     * @param second The ending point.
     * @return The distance, in kilometers, between the two points.
     */
    fun between(
        first: GeoPoint,
        second: GeoPoint,
    ): Double {
        val radLat1 = (first.latitude * PI) / 180
        val radLat2 = (second.latitude * PI) / 180

        val radLon1 = (first.longitude * PI) / 180
        val radLon2 = (second.longitude * PI) / 180

        val latCosines = cos(radLat1) * cos(radLat2)
        val latHaversine = sin((radLat2 - radLat1) / 2).pow(2)
        val lonHaversine = sin((radLon2 - radLon1) / 2).pow(2)

        val bigThetaHaversine = latHaversine + latCosines * lonHaversine

        return 2 * MEAN_SPHERICAL_EARTH_RADIUS * asin(sqrt(bigThetaHaversine))
    }
}
