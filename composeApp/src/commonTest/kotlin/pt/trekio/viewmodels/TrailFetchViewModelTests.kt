package pt.trekio.viewmodels

import pt.trekio.dto.TrailDto
import pt.trekio.dto.TrailListDto
import pt.trekio.services.SuccessfulTrailService
import pt.trekio.services.SuccessfulUserRepo
import pt.trekio.services.utils.TestTrail
import pt.trekio.services.utils.TestTrail.DISTANCE
import pt.trekio.services.utils.TestTrail.TID
import pt.trekio.services.utils.TestTrail.TRAIL_NAME
import pt.trekio.services.utils.TestUser.UID
import kotlin.test.BeforeTest

class TrailFetchViewModelTests {
    lateinit var viewModel: TrailFetchViewModel

    val expectedTrailList =
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
        )

    @BeforeTest
    fun setup() {
        viewModel = TrailFetchViewModel(SuccessfulUserRepo, SuccessfulTrailService)
    }
}
