package pt.trekio.dto

import kotlinx.serialization.Serializable
import pt.trekio.domain.Hike
import pt.trekio.domain.toDto

@Serializable
data class HikeListDto(
    val hikes: List<HikeDto>,
    val hasMore: Boolean,
)

fun Pair<List<Hike>, Boolean>.toDto() =
    HikeListDto(
        first.map(Hike::toDto),
        second,
    )
