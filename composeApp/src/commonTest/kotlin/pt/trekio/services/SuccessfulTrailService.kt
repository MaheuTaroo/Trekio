package pt.trekio.services

import pt.trekio.dto.ResultIdDto
import pt.trekio.dto.TrailDto
import pt.trekio.dto.TrailListDto
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.success
import pt.trekio.services.trails.TrailService
import pt.trekio.services.utils.TestTrail
import pt.trekio.services.utils.TestTrail.DISTANCE
import pt.trekio.services.utils.TestTrail.TID
import pt.trekio.services.utils.TestTrail.TRAIL_NAME
import pt.trekio.services.utils.TestUser.UID

object SuccessfulTrailService : TrailService {
    override suspend fun createTrail(
        name: String,
        start: GeoPoint,
        end: GeoPoint,
        path: List<GeoPoint>,
        parentId: ULong?,
    ): Either<String, ResultIdDto> = success(ResultIdDto(TID))

    override suspend fun importTrail(): Either<String, ResultIdDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getTrailDetails(id: ULong): Either<String, TrailDto> =
        success(
            TrailDto(
                id = TID,
                name = TRAIL_NAME,
                creator = UID,
                start = TestTrail.start,
                end = TestTrail.end,
                path = TestTrail.path,
                distance = DISTANCE,
                difficulty = TestTrail.difficulty,
                parent = null,
            ),
        )

    override suspend fun getTrailsOf(
        userId: ULong,
        page: ULong,
    ): Either<String, TrailListDto> =
        success(
            TrailListDto(
                listOf(
                    TrailDto(
                        id = TID,
                        name = TRAIL_NAME,
                        creator = UID,
                        start = TestTrail.start,
                        end = TestTrail.end,
                        path = TestTrail.path,
                        distance = DISTANCE,
                        difficulty = TestTrail.difficulty,
                        parent = null,
                    ),
                ),
            ),
        )

    override suspend fun getAllTrails(page: ULong): Either<String, TrailListDto> =
        success(
            TrailListDto(
                listOf(
                    TrailDto(
                        id = TID,
                        name = TRAIL_NAME,
                        creator = UID,
                        start = TestTrail.start,
                        end = TestTrail.end,
                        path = TestTrail.path,
                        distance = DISTANCE,
                        difficulty = TestTrail.difficulty,
                        parent = null,
                    ),
                ),
            ),
        )

    override suspend fun updateTrail(
        id: ULong,
        name: String,
        parentId: ULong?,
    ): Either<String, Unit> = success(Unit)

    override suspend fun deleteTrail(id: ULong): Either<String, Unit> = success(Unit)
}
