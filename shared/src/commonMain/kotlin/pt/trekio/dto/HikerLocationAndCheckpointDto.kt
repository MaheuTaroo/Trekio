package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class HikerLocationAndCheckpointDto(
    val uid: ULong,
    val currentLocation: GeoPointDto?,
    val lastCheckpoint: GeoPointDto?,
)

fun HikerLocationAndCheckpointDto.withoutCheckpoint() = HikerLocationNoticeDto(uid, currentLocation)
