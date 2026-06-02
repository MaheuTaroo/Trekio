package pt.trekio.dto

import kotlinx.serialization.Serializable
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailType

@Serializable
data class TrailDto(
    val name: String,
    val start: GeoPointDto,
    val end: GeoPointDto,
    val path: List<GeoPointDto>,
    val distance: Double,
    val type: TrailType,
    val difficulty: TrailDifficulty,
    val parent: ULong?,
)
