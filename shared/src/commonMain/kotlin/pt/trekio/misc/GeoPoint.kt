package pt.trekio.misc

import pt.trekio.dto.GeoPointDto

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
) {
    override fun toString() = "($latitude;$longitude;$altitude)"
}

fun String.toGeoPoint(): GeoPoint {
    if (!startsWith("(") || !endsWith(")")) {
        error("Incorrect geo-point format (expected \"(<latitude>;<longitude>;<altitude>)\", got \"$this\")")
    }

    val textCoords = split(";").toTypedArray()
    if (textCoords.size != 3) {
        error("Expected 3 coordinates from geo-point, found ${textCoords.size}")
    }

    if (textCoords.any(String::isNullOrBlank)) {
        error("One or more coordinates from the geopoint are blank")
    }

    textCoords[0] = textCoords[0].drop(1)
    textCoords[2] = textCoords[2].dropLast(1)

    val floatCoords =
        textCoords.map { coordinate ->
            val toParse = coordinate.trim()
            toParse.toDoubleOrNull() ?: error("Could not parse $toParse to a floating point value")
        }

    return GeoPoint(floatCoords[0], floatCoords[1], floatCoords[2])
}

fun GeoPoint.toDto() = GeoPointDto(latitude, longitude, altitude)

fun GeoPointDto.toGeoPoint() = GeoPoint(lat, lon, alt)
