package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class HikerLocationNoticeDto(
    val id: ULong,
    val currentLocation: GeoPointDto?,
)
