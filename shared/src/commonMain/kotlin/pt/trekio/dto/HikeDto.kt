package pt.trekio.dto

data class HikeDto(
    val id: ULong,
    val hiker: ULong,
    val trail: ULong,
    val entry: TrailPointDto,
    val exit: TrailPointDto?,
    val start: Long,
    val finish: Long?,
)
