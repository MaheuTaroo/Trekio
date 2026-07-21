package pt.trekio.misc

import io.github.tiagopraia.kmp.mapbox.GeographicPoint
import pt.trekio.dto.GeoPointDto

fun GeographicPoint.toGeoPoint() = GeoPoint(latitude, longitude, altitude)

fun GeoPointDto.toGeographicPoint() = GeographicPoint(lat, lon, alt)
