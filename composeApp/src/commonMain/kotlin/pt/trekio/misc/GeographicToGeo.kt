package pt.trekio.misc

import io.github.tiagopraia.kmp.mapbox.GeographicPoint

fun GeographicPoint.toGeoPoint() = GeoPoint(latitude, longitude, altitude)
