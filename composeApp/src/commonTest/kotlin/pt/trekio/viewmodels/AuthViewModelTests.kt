package pt.trekio.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pt.trekio.services.FailingService
import pt.trekio.services.FailingService.ERROR
import pt.trekio.services.SuccessfulUserService
import pt.trekio.services.utils.TestUser.EMAIL
import pt.trekio.services.utils.TestUser.GOOGLE_SUCCESS
import pt.trekio.services.utils.TestUser.PASSWORD
import pt.trekio.services.utils.TestUser.USERNAME
import pt.trekio.utils.SuspendingLatch
import pt.trekio.viewmodels.states.AuthState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthViewModelTests {
    lateinit var viewModel: AuthViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = AuthViewModel(SuccessfulUserService)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun cleanup() {
        viewModel.resetState()
        Dispatchers.resetMain()
    }

    @Test
    fun `ViewModel starts and stays in Idle if nothing is executed`() =
        runTest {
            var result: AuthState = viewModel.state.value
            assertEquals(AuthState.Idle, result)
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

            assertEquals(AuthState.Idle, result)
        }

    @Test
    fun `ViewModel goes to SignUpError state in register if username, email or password are invalid or passwords do not match`() =
        runTest {
            viewModel.register("", EMAIL, PASSWORD, PASSWORD)
            assertTrue(viewModel.state.value is AuthState.SignUpError)
            viewModel.resetState()

            viewModel.register(USERNAME, "", PASSWORD, PASSWORD)
            assertTrue(viewModel.state.value is AuthState.SignUpError)
            viewModel.resetState()

            viewModel.register(USERNAME, EMAIL, "", PASSWORD)
            assertTrue(viewModel.state.value is AuthState.SignUpError)
            viewModel.resetState()

            viewModel.register(USERNAME, EMAIL, PASSWORD, "")
            assertTrue(viewModel.state.value is AuthState.SignUpError)
            assertEquals("Passwords do not match", (viewModel.state.value as AuthState.SignUpError).message)
            viewModel.resetState()
        }

    @Test
    fun `ViewModel goes to Loading state in register if it passes first verifications`() =
        runTest {
            var result: AuthState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is AuthState.Loading) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.register(USERNAME, EMAIL, PASSWORD, PASSWORD)

            latch.await()
            action.cancel()

            assertEquals(AuthState.Loading, result)
        }

    @Test
    fun `ViewModel goes to SignUpError state in register if there's an error incoming from service`() =
        runTest {
            val vm = AuthViewModel(FailingService)
            var result: AuthState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is AuthState.SignUpError) {
                            result = it
                            latch.open()
                        }
                    }
                }

            vm.register(USERNAME, EMAIL, PASSWORD, PASSWORD)

            latch.await()
            action.cancel()

            assertTrue(result is AuthState.SignUpError)
            assertEquals(ERROR, result.message)
        }

    @Test
    fun `ViewModel goes to Success state in register if all is correct`() =
        runTest {
            var result: AuthState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is AuthState.Success) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.register(USERNAME, EMAIL, PASSWORD, PASSWORD)

            latch.await()
            action.cancel()

            assertTrue(result is AuthState.Success)
        }

    @Test
    fun `ViewModel goes to LoginError state in login if email or password are invalid`() =
        runTest {
            viewModel.login("", PASSWORD)
            assertTrue(viewModel.state.value is AuthState.LoginError)
            viewModel.resetState()

            viewModel.login(EMAIL, "")
            assertTrue(viewModel.state.value is AuthState.LoginError)
        }

    @Test
    fun `ViewModel goes to Loading state in login if it passes first verifications`() =
        runTest {
            var result: AuthState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is AuthState.Loading) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.login(EMAIL, PASSWORD)

            latch.await()
            action.cancel()

            assertEquals(AuthState.Loading, result)
        }

    @Test
    fun `ViewModel goes to LoginError state in login if there's an error incoming from service`() =
        runTest {
            val vm = AuthViewModel(FailingService)
            var result: AuthState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is AuthState.LoginError) {
                            result = it
                            latch.open()
                        }
                    }
                }

            vm.login(EMAIL, PASSWORD)

            latch.await()
            action.cancel()

            assertTrue(result is AuthState.LoginError)
            assertEquals(ERROR, result.message)
        }

    @Test
    fun `ViewModel goes to Success state in login if all is correct`() =
        runTest {
            var result: AuthState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is AuthState.Success) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.login(EMAIL, PASSWORD)

            latch.await()
            action.cancel()

            assertTrue(result is AuthState.Success)
        }

    @Test
    fun `Google state in ViewModel goes to null if there's an error`() =
        runTest {
            val vm = AuthViewModel(FailingService)
            vm.googleAuth()
            assertNull(vm.googleState.value)
        }

    @Test
    fun `Google state in ViewModel goes to google redirect url if successful`() =
        runTest {
            var result: String? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.googleState.collect {
                        if (it != null) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.googleAuth()

            latch.await()
            action.cancel()

            assertEquals(GOOGLE_SUCCESS, result)
        }

    @Test
    fun `Google state in ViewModel goes to null if cleanupGoogle function is called`() =
        runTest {
            var result: String? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.googleState.collect {
                        if (it != null) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.googleAuth()

            latch.await()
            action.cancel()
            assertEquals(GOOGLE_SUCCESS, result)

            val secondLatch = SuspendingLatch()

            val secondAction =
                launch {
                    viewModel.googleState.collect {
                        if (it == null) {
                            result = it
                            secondLatch.open()
                        }
                    }
                }

            viewModel.cleanupGoogle()

            secondLatch.await()
            secondAction.cancel()
            assertNull(result)
        }

    @Test
    fun `ViewModel goes to Loading state in updateUser if it just started`() =
        runTest {
            var result: AuthState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is AuthState.Loading) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.updateUser(USERNAME + "1", null)

            latch.await()
            action.cancel()
            assertTrue(result is AuthState.Loading)
        }

    @Test
    fun `ViewModel goes to OAuthError state in updateUser if there's an error incoming from service`() =
        runTest {
            val vm = AuthViewModel(FailingService)
            var result: AuthState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    vm.state.collect {
                        if (it is AuthState.OAuthError) {
                            result = it
                            latch.open()
                        }
                    }
                }

            vm.updateUser(USERNAME + "1", null)

            latch.await()
            action.cancel()
            assertTrue(result is AuthState.OAuthError)
            assertEquals(ERROR, result.message)
        }

    @Test
    fun `ViewModel goes to Success state in updateUser if all is correct`() =
        runTest {
            var result: AuthState? = null
            val latch = SuspendingLatch()

            val action =
                launch {
                    viewModel.state.collect {
                        if (it is AuthState.Success) {
                            result = it
                            latch.open()
                        }
                    }
                }

            viewModel.updateUser(USERNAME + "1", null)

            latch.await()
            action.cancel()
            assertTrue(result is AuthState.Success)
        }

    @Test
    fun `ViewModel goes back to Idle state if resetState is called`() =
        runTest {
            viewModel.login("", PASSWORD)
            assertTrue(viewModel.state.value is AuthState.LoginError)

            viewModel.resetState()
            assertTrue(viewModel.state.value is AuthState.Idle)
        }
}
