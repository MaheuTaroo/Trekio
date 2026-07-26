package pt.trekio.services

import pt.trekio.dto.HikeDto
import pt.trekio.dto.HikeListDto
import pt.trekio.misc.Either
import pt.trekio.misc.WebSocketCommunicator
import pt.trekio.misc.success
import pt.trekio.services.hikes.HikeService
import pt.trekio.services.utils.TestTrail
import pt.trekio.services.utils.TestTrail.TID
import pt.trekio.services.utils.TestUser.UID

object SuccessfulHikeService : HikeService {
    override suspend fun startHike(
        trailId: ULong,
        isFirstPoint: Boolean,
    ): Either<String, WebSocketCommunicator> {
        TODO("Not yet implemented")
    }

    override suspend fun getMyFinishedHikes(page: ULong): Either<String, HikeListDto> =
        success(
            value =
                HikeListDto(
                    hikes =
                        listOf(
                            HikeDto(
                                id = 1UL,
                                hiker = UID,
                                trail = TID,
                                entry = TestTrail.start,
                                exit = TestTrail.end,
                                start = 0L,
                                finish = 600L,
                            ),
                        ),
                    hasMore = false,
                ),
        )
}
