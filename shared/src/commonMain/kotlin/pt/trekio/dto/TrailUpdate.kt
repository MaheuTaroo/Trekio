package pt.trekio.dto

import kotlinx.serialization.Serializable
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailType

@Serializable
data class TrailUpdate(
    val name: String,
    val type: TrailType,
    val difficulty: TrailDifficulty,
    val parent: ULong?,
)
