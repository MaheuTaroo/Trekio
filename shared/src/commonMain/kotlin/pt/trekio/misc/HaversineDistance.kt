package pt.trekio.misc

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object HaversineDistance {
    /**
     * The mean spherical radius of the Earth, in kilometers.
     */
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

    private fun bearing(
        from: GeoPoint,
        to: GeoPoint,
    ): Double {
        val lat1 = (from.latitude * PI) / 180
        val lat2 = (to.latitude * PI) / 180
        val dLon = ((to.longitude - from.longitude) * PI) / 180

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return atan2(y, x)
    }

    fun distanceToSegment(
        point: GeoPoint,
        segStart: GeoPoint,
        segEnd: GeoPoint,
    ): Double {
        val segmentLengthKm = between(segStart, segEnd)
        if (segmentLengthKm == 0.0) return between(segStart, point) * 1000.0

        val d13 = between(segStart, point) / MEAN_SPHERICAL_EARTH_RADIUS
        val brng13 = bearing(segStart, point)
        val brng12 = bearing(segStart, segEnd)

        val crossTrackKm = asin(sin(d13) * sin(brng13 - brng12)) * MEAN_SPHERICAL_EARTH_RADIUS

        val ratio = (cos(d13) / cos(crossTrackKm / MEAN_SPHERICAL_EARTH_RADIUS)).coerceIn(-1.0, 1.0)
        val alongTrackKm = acos(ratio) * MEAN_SPHERICAL_EARTH_RADIUS

        val distanceKm =
            when {
                alongTrackKm < 0.0 -> between(segStart, point)
                alongTrackKm > segmentLengthKm -> between(segEnd, point)
                else -> abs(crossTrackKm)
            }

        return distanceKm * 1000.0
    }
}
