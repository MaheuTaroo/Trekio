package pt.trekio.dto

import kotlinx.serialization.Serializable
import pt.trekio.misc.TrailDifficulty

@Serializable
data class TrailDto(
    val id: ULong,
    val name: String,
    val start: GeoPointDto,
    val end: GeoPointDto,
    val path: List<GeoPointDto>,
    val distance: Double,
    val difficulty: TrailDifficulty,
    val parent: ULong?,
)
