package pt.trekio.dto

import kotlinx.serialization.Serializable
import pt.trekio.domain.Trail
import pt.trekio.domain.toDto

@Serializable
data class TrailListDto(
    val trails: List<TrailDto>,
)

fun List<Trail>.toDto() = TrailListDto(map(Trail::toDto))
