package pt.trekio.misc

data class HikeInfo(
    val id: ULong,
    val trailName: String,
    val distance: Double,
    val difficulty: TrailDifficulty,
    val timeSpent: Long,
)
