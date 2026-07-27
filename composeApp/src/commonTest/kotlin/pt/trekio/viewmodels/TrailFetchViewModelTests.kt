package pt.trekio.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pt.trekio.dto.TrailDto
import pt.trekio.dto.TrailListDto
import pt.trekio.services.FailingService
import pt.trekio.services.FailingService.ERROR
import pt.trekio.services.SuccessfulTrailService
import pt.trekio.services.SuccessfulUserRepo
import pt.trekio.services.utils.TestTrail
import pt.trekio.services.utils.TestTrail.DISTANCE
import pt.trekio.services.utils.TestTrail.TID
import pt.trekio.services.utils.TestTrail.TRAIL_NAME
import pt.trekio.services.utils.TestUser.UID
import pt.trekio.utils.SuspendingLatch
import pt.trekio.viewmodels.states.TrailFetchState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = TrailFetchViewModel(SuccessfulUserRepo, SuccessfulTrailService)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun cleanup() {
        viewModel = TrailFetchViewModel(SuccessfulUserRepo, SuccessfulTrailService)
        Dispatchers.resetMain()
    }

    @Test
    fun `ViewModel starts and stays in Idle state if nothing is executed`() =
        runTest {
            var result: TrailFetchState = viewModel.state.value
            assertTrue(result is TrailFetchState.Idle)

            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        result = it
                        latch.open()
                    }
                }

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.Idle)
        }

    @Test
    fun `ViewModel goes into Loading state immediately after executing fetchPage`() =
        runTest {
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is TrailFetchState.Loading) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.fetchPage()

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.Loading)
        }

    @Test
    fun `ViewModel goes into Error state if there's an error incoming from service about the execution of fetchPage`() =
        runTest {
            val vm = TrailFetchViewModel(FailingService, FailingService)
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is TrailFetchState.Error) {
                            result = it
                            latch.open()
                        }
                    }
                }

            vm.fetchPage()

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.Error)
            assertEquals(ERROR, result.message)
        }

    @Test
    fun `ViewModel goes into TrailsSuccess state if the execution of fetchPage is successful`() =
        runTest {
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is TrailFetchState.TrailsSuccess) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.fetchPage()

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.TrailsSuccess)
            assertEquals(expectedTrailList.trails, result.trails)
        }

    @Test
    fun `ViewModel goes into Loading state immediately after executing fetchTrailsByName`() =
        runTest {
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is TrailFetchState.Loading) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.fetchTrailsByName(TRAIL_NAME)

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.Loading)
        }

    @Test
    fun `ViewModel goes into Error state if there's an error incoming from service about the execution of fetchTrailsByName`() =
        runTest {
            val vm = TrailFetchViewModel(SuccessfulUserRepo, FailingService)
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is TrailFetchState.Error) {
                            result = it
                            latch.open()
                        }
                    }
                }

            vm.fetchTrailsByName(TRAIL_NAME)

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.Error)
            assertEquals(ERROR, result.message)
        }

    @Test
    fun `ViewModel goes into TrailsSuccess state if the execution of fetchTrailsByName is successful`() =
        runTest {
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is TrailFetchState.TrailsSuccess) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.fetchTrailsByName(TRAIL_NAME)

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.TrailsSuccess)
            assertEquals(expectedTrailList.trails, result.trails)
        }

    @Test
    fun `ViewModel goes into Loading state immediately after executing fetchPersonalTrails`() =
        runTest {
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is TrailFetchState.Loading) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.fetchPersonalTrails()

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.Loading)
        }

    @Test
    fun `ViewModel goes into Error state if there's an error about the user's details gathering at fetchPersonalTrails`() =
        runTest {
            val vm = TrailFetchViewModel(FailingService, FailingService)
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is TrailFetchState.Error) {
                            result = it
                            latch.open()
                        }
                    }
                }

            vm.fetchPersonalTrails()

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.Error)
            assertEquals("User Not Found", result.message)
        }

    @Test
    fun `ViewModel goes into Error state if there's an error incoming from service about the execution of fetchPersonalTrails`() =
        runTest {
            val vm = TrailFetchViewModel(SuccessfulUserRepo, FailingService)
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is TrailFetchState.Error) {
                            result = it
                            latch.open()
                        }
                    }
                }

            vm.fetchPersonalTrails()

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.Error)
            assertEquals(ERROR, result.message)
        }

    @Test
    fun `ViewModel goes into TrailsSuccess state if the execution of fetchPersonalTrails is successful`() =
        runTest {
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is TrailFetchState.TrailsSuccess) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.fetchPersonalTrails()

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.TrailsSuccess)
            assertEquals(expectedTrailList.trails, result.trails)
        }

    @Test
    fun `ViewModel goes into Loading state immediately after executing deleteTrail`() =
        runTest {
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is TrailFetchState.Loading) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.deleteTrail(TID)

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.Loading)
        }

    @Test
    fun `ViewModel goes into Error state if there's an error incoming from service about the execution of deleteTrail`() =
        runTest {
            val vm = TrailFetchViewModel(SuccessfulUserRepo, FailingService)
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is TrailFetchState.Error) {
                            result = it
                            latch.open()
                        }
                    }
                }

            vm.deleteTrail(TID)

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.Error)
            assertEquals(ERROR, result.message)
        }

    @Test
    fun `ViewModel goes into Success state if the execution of deleteTrail is successful`() =
        runTest {
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is TrailFetchState.Success) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.deleteTrail(TID)

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.Success)
        }

    @Test
    fun `ViewModel goes into Loading state immediately after executing updateTrail`() =
        runTest {
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is TrailFetchState.Loading) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.updateTrail(TID, TRAIL_NAME)

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.Loading)
        }

    @Test
    fun `ViewModel goes into UpdateError state if there's an error incoming from service about the execution of updateTrail`() =
        runTest {
            val vm = TrailFetchViewModel(SuccessfulUserRepo, FailingService)
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is TrailFetchState.UpdateError) {
                            result = it
                            latch.open()
                        }
                    }
                }

            vm.updateTrail(TID, TRAIL_NAME)

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.UpdateError)
            assertEquals(ERROR, result.message)
        }

    @Test
    fun `ViewModel goes into Success state if the execution of updateTrail is successful`() =
        runTest {
            var result: TrailFetchState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is TrailFetchState.Success) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.updateTrail(TID, TRAIL_NAME)

            latch.await()
            action.cancel()
            assertTrue(result is TrailFetchState.Success)
        }
}
