package pt.trekio.domain

import pt.trekio.dto.TrailDto
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailName
import pt.trekio.misc.toDto

data class Trail(
    val trailId: ULong,
    val name: TrailName,
    val creator: ULong,
    val start: GeoPoint,
    val end: GeoPoint,
    val path: List<GeoPoint>,
    val distance: Double,
    val difficulty: TrailDifficulty,
    val parent: ULong?,
)

fun Trail.toDto() =
    TrailDto(
        trailId,
        name.value,
        start.toDto(),
        end.toDto(),
        path.map(GeoPoint::toDto),
        distance,
        difficulty,
        parent,
    )
