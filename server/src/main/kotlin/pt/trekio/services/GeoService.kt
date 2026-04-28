package pt.trekio.services

import pt.trekio.misc.GeoPoint
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

abstract class GeoService : Service() {
    protected val meanSphericalEarthRadius = 6371.0088

    /**
     * Calculates the distance between two points, following the
     * Haversine formula. Does not use the point's altitude.
     *
     * Reference: [Haversine formula](https://en.wikipedia.org/wiki/Haversine_formula#Formulation)
     *
     * @param start The starting point.
     * @param end The ending point.
     * @return The distance, in kilometers, between the two points.
     */
    protected fun haversineDistance(
        start: GeoPoint,
        end: GeoPoint,
    ): Double {
        val radLat1 = (start.latitude * PI) / 180
        val radLat2 = (end.latitude * PI) / 180

        val radLon1 = (start.longitude * PI) / 180
        val radLon2 = (end.longitude * PI) / 180

        val latCosines = cos(radLat1) * cos(radLat2)
        val latHaversine = sin((radLat2 - radLat1) / 2).pow(2)
        val lonHaversine = sin((radLon2 - radLon1) / 2).pow(2)

        val bigThetaHaversine = latHaversine + latCosines * lonHaversine

        return 2 * meanSphericalEarthRadius * asin(sqrt(bigThetaHaversine))
    }
}
