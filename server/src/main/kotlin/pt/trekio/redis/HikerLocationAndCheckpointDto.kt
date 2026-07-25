package pt.trekio.redis

import pt.trekio.dto.HikerLocationNoticeDto
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.toDto

data class HikerLocationAndCheckpointDto(
    val uid: ULong,
    val currentLocation: GeoPoint?,
    val lastCheckpoint: GeoPoint?,
)

fun HikerLocationAndCheckpointDto.withoutCheckpoint() = HikerLocationNoticeDto(uid, currentLocation?.toDto())
