package pt.trekio.dto

import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailType

data class TrailUpdate(
    val name: String,
    val type: TrailType,
    val difficulty: TrailDifficulty,
    val parent: ULong?,
)
