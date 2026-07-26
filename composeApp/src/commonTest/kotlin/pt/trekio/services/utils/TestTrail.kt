package pt.trekio.services.utils

import pt.trekio.dto.GeoPointDto
import pt.trekio.misc.TrailDifficulty

object TestTrail {
    const val TID: ULong = 1UL
    const val TRAIL_NAME: String = "Test Trail"
    val start: GeoPointDto = GeoPointDto(0.0, 0.0, 0.0)
    val end: GeoPointDto = GeoPointDto(1.0, 1.0, 1.0)
    val path: List<GeoPointDto> = emptyList()
    const val DISTANCE: Double = 2.0
    val difficulty: TrailDifficulty = TrailDifficulty.BEGINNER
}
