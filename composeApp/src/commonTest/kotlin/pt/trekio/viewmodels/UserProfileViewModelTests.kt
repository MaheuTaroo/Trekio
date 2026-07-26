package pt.trekio.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pt.trekio.dto.HikeDto
import pt.trekio.dto.HikeListDto
import pt.trekio.misc.Either
import pt.trekio.misc.WebSocketCommunicator
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.services.FailingService
import pt.trekio.services.FailingService.ERROR
import pt.trekio.services.SuccessfulHikeService
import pt.trekio.services.SuccessfulTrailService
import pt.trekio.services.SuccessfulUserRepo
import pt.trekio.services.SuccessfulUserService
import pt.trekio.services.hikes.HikeService
import pt.trekio.services.utils.TestTrail
import pt.trekio.services.utils.TestTrail.TID
import pt.trekio.services.utils.TestUser.UID
import pt.trekio.utils.SuspendingLatch
import pt.trekio.viewmodels.states.UserProfileState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserProfileViewModelTests {
    lateinit var viewModel: UserProfileViewModel

    object FailingHikeTestService : HikeService {
        override suspend fun startHike(
            trailId: ULong,
            isFirstPoint: Boolean,
        ): Either<String, WebSocketCommunicator> = failure(ERROR)

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
                                    finish = null,
                                ),
                            ),
                        hasMore = false,
                    ),
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = UserProfileViewModel(SuccessfulUserService, SuccessfulTrailService, SuccessfulHikeService, SuccessfulUserRepo)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun cleanup() {
        viewModel = UserProfileViewModel(SuccessfulUserService, SuccessfulTrailService, SuccessfulHikeService, SuccessfulUserRepo)
        Dispatchers.resetMain()
    }

    @Test
    fun `ViewModel goes immediately to Loading state when initiating`() =
        runTest {
            val vm = UserProfileViewModel(SuccessfulUserService, SuccessfulTrailService, SuccessfulHikeService, SuccessfulUserRepo)
            assertEquals(UserProfileState.Loading, vm.state.value)
        }

    @Test
    fun `ViewModel goes to Error state if it can't get user's details`() =
        runTest {
            val vm = UserProfileViewModel(SuccessfulUserService, SuccessfulTrailService, SuccessfulHikeService, FailingService)
            var result: UserProfileState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is UserProfileState.Error) {
                            result = it
                            latch.open()
                        }
                    }
                }

            latch.await()
            action.cancel()

            assertEquals(UserProfileState.Error("Could not find own statistics"), result)
        }

    @Test
    fun `ViewModel goes to Error state if it can't get user's statistics`() =
        runTest {
            val vm = UserProfileViewModel(FailingService, SuccessfulTrailService, SuccessfulHikeService, SuccessfulUserRepo)
            var result: UserProfileState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is UserProfileState.Error) {
                            result = it
                            latch.open()
                        }
                    }
                }

            latch.await()
            action.cancel()

            assertTrue(result is UserProfileState.Error)
            assertEquals(ERROR, result.message)
        }

    @Test
    fun `ViewModel goes to Error state if it can't fetch hikes`() =
        runTest {
            val vm = UserProfileViewModel(SuccessfulUserService, SuccessfulTrailService, FailingService, SuccessfulUserRepo)
            var result: UserProfileState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is UserProfileState.Error) {
                            result = it
                            latch.open()
                        }
                    }
                }

            latch.await()
            action.cancel()

            assertTrue(result is UserProfileState.Error)
            assertEquals(ERROR, result.message)
        }

    @Test
    fun `ViewModel goes to Error state if one of the fetched hikes is not finished`() =
        runTest {
            val vm = UserProfileViewModel(SuccessfulUserService, SuccessfulTrailService, FailingHikeTestService, SuccessfulUserRepo)
            var result: UserProfileState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is UserProfileState.Error) {
                            result = it
                            latch.open()
                        }
                    }
                }

            latch.await()
            action.cancel()

            assertTrue(result is UserProfileState.Error)
            assertEquals("One of the hikes is not finished yet", result.message)
        }

    @Test
    fun `ViewModel goes to Error state if one of the hikes' trail details can't be fetched`() =
        runTest {
            val vm = UserProfileViewModel(SuccessfulUserService, FailingService, SuccessfulHikeService, SuccessfulUserRepo)
            var result: UserProfileState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is UserProfileState.Error) {
                            result = it
                            latch.open()
                        }
                    }
                }

            latch.await()
            action.cancel()

            assertTrue(result is UserProfileState.Error)
            assertEquals(ERROR, result.message)
        }

    @Test
    fun `ViewModel goes to Success state if all data is correct`() =
        runTest {
            var result: UserProfileState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is UserProfileState.Success) {
                            result = it
                            latch.open()
                        }
                    }
                }

            latch.await()
            action.cancel()

            assertEquals(UserProfileState.Success, result)
        }
}
