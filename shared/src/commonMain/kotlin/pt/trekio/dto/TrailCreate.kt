package pt.trekio.dto

import kotlinx.serialization.Serializable
import pt.trekio.misc.TrailDifficulty

@Serializable
data class TrailCreate(
    val name: String,
    val start: GeoPointDto,
    val end: GeoPointDto,
    val path: List<GeoPointDto>,
    val isPrivate: Boolean,
    val firstReview: TrailDifficulty,
    val parentId: ULong?,
)
