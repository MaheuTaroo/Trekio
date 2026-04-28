package pt.trekio.domain

import pt.trekio.dto.HikeDto
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.toDto
import kotlin.time.Instant

data class Hike(
    val id: ULong,
    val hiker: ULong,
    val trail: ULong,
    val entry: GeoPoint,
    val exit: GeoPoint?,
    val start: Instant,
    val finish: Instant?,
)

fun Hike.toDto() =
    HikeDto(
        id,
        hiker,
        trail,
        entry.toDto(),
        exit?.toDto(),
        start.toEpochMilliseconds(),
        finish?.toEpochMilliseconds(),
    )
