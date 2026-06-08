package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class HikeDto(
    val id: ULong,
    val hiker: ULong,
    val trail: ULong,
    val entry: GeoPointDto,
    val exit: GeoPointDto?,
    val start: Long,
    val finish: Long?,
)
