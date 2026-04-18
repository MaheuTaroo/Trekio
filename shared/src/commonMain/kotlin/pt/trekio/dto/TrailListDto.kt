package pt.trekio.dto

import pt.trekio.domain.Trail
import pt.trekio.domain.toDto

data class TrailListDto(
    val trails: List<TrailDto>,
)

fun List<Trail>.toDto() = TrailListDto(map(Trail::toDto))
