package pt.trekio.dto

import kotlinx.serialization.Serializable
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailType

@Serializable
data class TrailDto(
    val name: String,
    val start: TrailPointDto,
    val end: TrailPointDto,
    val path: List<TrailPointDto>,
    val type: TrailType,
    val difficulty: TrailDifficulty,
    val parent: ULong?,
)
