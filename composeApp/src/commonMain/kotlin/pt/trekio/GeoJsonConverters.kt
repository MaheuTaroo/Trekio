package pt.trekio

import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import pt.trekio.dto.TrailDto
import pt.trekio.dto.TrailPointDto

fun TrailPointDto.toGeoJsonPosition() = Position(lon, lat, alt)
fun TrailPointDto.toGeoJsonPoint() = Point(toGeoJsonPosition())

fun TrailDto.toGeoJsonLineString(): LineString {
    val list = mutableListOf<Position>()
    list.add(start.toGeoJsonPosition())
    list.addAll(path.map(TrailPointDto::toGeoJsonPosition))
    list.add(end.toGeoJsonPosition())

    return LineString(list)
}