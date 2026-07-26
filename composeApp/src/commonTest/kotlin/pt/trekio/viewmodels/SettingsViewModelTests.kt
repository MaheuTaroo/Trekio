package pt.trekio.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pt.trekio.misc.Language
import pt.trekio.misc.Metric
import pt.trekio.services.FailingService
import pt.trekio.services.FailingService.ERROR
import pt.trekio.services.SuccessfulSettingsRepository
import pt.trekio.services.SuccessfulUserService
import pt.trekio.services.utils.TestSettings.language
import pt.trekio.services.utils.TestSettings.metric
import pt.trekio.services.utils.TestSettings.theme
import pt.trekio.services.utils.TestUser.USERNAME
import pt.trekio.ui.theme.ThemeMode
import pt.trekio.utils.SuspendingLatch
import pt.trekio.viewmodels.states.SettingsState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsViewModelTests {
    lateinit var viewModel: SettingsViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = SettingsViewModel(SuccessfulSettingsRepository, SuccessfulUserService)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun cleanup() {
        viewModel.resetState()
        Dispatchers.resetMain()
    }

    @Test
    fun `ViewModel gets and stores theme in respective flow after invoking setThemeMode`() =
        runTest {
            assertEquals(theme, viewModel.theme.value)

            viewModel.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, viewModel.theme.value)
        }

    @Test
    fun `ViewModel gets and stores language in respective flow after invoking setLanguage`() =
        runTest {
            assertEquals(language, viewModel.language.value)

            viewModel.setLanguage(Language.Portuguese)
            assertEquals(Language.Portuguese, viewModel.language.value)
        }

    @Test
    fun `ViewModel gets and stores metric in respective flow after invoking setMetric`() =
        runTest {
            assertEquals(metric, viewModel.metric.value)

            viewModel.setMetric(Metric.Miles)
            assertEquals(Metric.Miles, viewModel.metric.value)
        }

    @Test
    fun `ViewModel goes to Loading state if updateUser action just started`() =
        runTest {
            var result: SettingsState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is SettingsState.Loading) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.updateUser(USERNAME + "1", null)

            latch.await()
            action.cancel()
            assertTrue(result is SettingsState.Loading)
        }

    @Test
    fun `ViewModel goes to UpdateError state if updateUser action encounters an error`() =
        runTest {
            val vm = SettingsViewModel(SuccessfulSettingsRepository, FailingService)
            var result: SettingsState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is SettingsState.UpdateError) {
                            result = it
                            latch.open()
                        }
                    }
                }

            vm.updateUser(USERNAME + "1", null)

            latch.await()
            action.cancel()
            assertTrue(result is SettingsState.UpdateError)
            assertEquals(ERROR, result.message)
        }

    @Test
    fun `ViewModel goes to Updated state if updateUser action succeeds`() =
        runTest {
            var result: SettingsState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is SettingsState.Updated) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.updateUser(USERNAME + "1", null)

            latch.await()
            action.cancel()
            assertTrue(result is SettingsState.Updated)
        }

    @Test
    fun `ViewModel goes to Loading state if logoutUser action just started`() =
        runTest {
            var result: SettingsState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is SettingsState.Loading) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.logoutUser()

            latch.await()
            action.cancel()
            assertTrue(result is SettingsState.Loading)
        }

    @Test
    fun `ViewModel goes to LogoutError state if logoutUser action encounters an error`() =
        runTest {
            val vm = SettingsViewModel(SuccessfulSettingsRepository, FailingService)
            var result: SettingsState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is SettingsState.LogoutError) {
                            result = it
                            latch.open()
                        }
                    }
                }

            vm.logoutUser()

            latch.await()
            action.cancel()
            assertTrue(result is SettingsState.LogoutError)
            assertEquals(ERROR, result.message)
        }

    @Test
    fun `ViewModel goes to LoggedOut state if logout action succeeds`() =
        runTest {
            var result: SettingsState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is SettingsState.LoggedOut) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.logoutUser()

            latch.await()
            action.cancel()
            assertTrue(result is SettingsState.LoggedOut)
        }

    @Test
    fun `ViewModel goes to Loading state if deleteUser action just started`() =
        runTest {
            var result: SettingsState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is SettingsState.Loading) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.deleteUser()

            latch.await()
            action.cancel()
            assertTrue(result is SettingsState.Loading)
        }

    @Test
    fun `ViewModel goes to DeleteError state if deleteUser action encounters an error`() =
        runTest {
            val vm = SettingsViewModel(SuccessfulSettingsRepository, FailingService)
            var result: SettingsState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is SettingsState.DeleteError) {
                            result = it
                            latch.open()
                        }
                    }
                }

            vm.deleteUser()

            latch.await()
            action.cancel()
            assertTrue(result is SettingsState.DeleteError)
            assertEquals(ERROR, result.message)
        }

    @Test
    fun `ViewModel goes to Deleted state if deleteUser action succeeds`() =
        runTest {
            var result: SettingsState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is SettingsState.Deleted) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.deleteUser()

            latch.await()
            action.cancel()
            assertTrue(result is SettingsState.Deleted)
        }

    @Test
    fun `ViewModel goes back to Idle state if resetState action is called`() =
        runTest {
            var result: SettingsState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is SettingsState.LoggedOut) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.logoutUser()

            latch.await()
            action.cancel()
            assertTrue(result is SettingsState.LoggedOut)

            viewModel.resetState()
            assertTrue(viewModel.state.value is SettingsState.Idle)
        }
}
