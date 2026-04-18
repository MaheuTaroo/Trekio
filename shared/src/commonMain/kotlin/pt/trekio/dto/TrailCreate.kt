package pt.trekio.dto

import pt.trekio.misc.TrailDifficulty

data class TrailCreate(
    val name: String,
    val start: TrailPointDto,
    val end: TrailPointDto,
    val path: List<TrailPointDto>,
    val isPrivate: Boolean,
    val firstReview: TrailDifficulty,
    val parentId: ULong?,
)
