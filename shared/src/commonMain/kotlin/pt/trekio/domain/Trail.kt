package pt.trekio.domain

import pt.trekio.dto.TrailDto
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailName
import pt.trekio.misc.TrailType
import pt.trekio.misc.toDto

data class Trail(
    val trailId: ULong,
    val name: TrailName,
    val creator: ULong,
    val start: GeoPoint,
    val end: GeoPoint,
    val path: List<GeoPoint>,
    val distance: Double,
    val type: TrailType,
    val difficulty: TrailDifficulty,
    val parent: ULong?,
)

fun Trail.toDto() =
    TrailDto(
        name.value,
        start.toDto(),
        end.toDto(),
        path.map(GeoPoint::toDto),
        distance,
        type,
        difficulty,
        parent,
    )
