package pt.trekio.dto

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport.Ignore

data class TrailPointDto(
    val lat: Double,
    val lon: Double,
    val alt: Double,
) {
    @OptIn(ExperimentalJsExport::class)
    @Ignore
    companion object {
        @Ignore
        fun serializer() = trailPointSerializer
    }
}

fun TrailPointDto.toGeoJsonArray() = arrayOf(lon, lat, alt)

fun Array<Double>.toTrailPoint(): TrailPointDto {
    require(size == 3)
    return TrailPointDto(this[1], this[0], this[2])
}
