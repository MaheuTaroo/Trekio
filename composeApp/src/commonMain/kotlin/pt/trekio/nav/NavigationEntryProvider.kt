package pt.trekio.nav

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import pt.trekio.dto.TrailDto
import pt.trekio.repos.UserRepository
import pt.trekio.services.hikes.HikeService
import pt.trekio.services.trails.TrailService
import pt.trekio.services.user.UserService
import pt.trekio.ui.AuthScreen
import pt.trekio.ui.MapScreen
import pt.trekio.ui.SettingsScreen
import pt.trekio.ui.TestHikingScreen
import pt.trekio.ui.TitleScreen
import pt.trekio.ui.TrailsScreen
import pt.trekio.ui.UserProfileScreen
import pt.trekio.viewmodels.AuthViewModel
import pt.trekio.viewmodels.MapViewModel
import pt.trekio.viewmodels.SettingsViewModel
import pt.trekio.viewmodels.TestHikingViewModel
import pt.trekio.viewmodels.TrailFetchViewModel
import pt.trekio.viewmodels.UserProfileViewModel

fun navigationEntryProvider(
    userService: UserService,
    trailService: TrailService,
    hikeService: HikeService,
    onUserProfile: () -> Unit,
    onBack: () -> Unit,
    onTrails: () -> Unit,
    onToAuthenticate: () -> Unit,
    onAuth: () -> Unit,
    onUserDelete: () -> Unit,
    onLogout: () -> Unit,
    onHike: (TrailDto) -> Unit,
    onHikeStopped: () -> Unit,
    onSettings: () -> Unit,
    settingsVm: SettingsViewModel,
    onLoggedIn: () -> Unit,
    userRepo: UserRepository,
): (Route) -> NavEntry<Route> =
    { key ->
        when (key) {
            Route.Title ->
                NavEntry(key) {
                    TitleScreen(
                        onAuthenticateClick = onToAuthenticate,
                        onLoggedIn = onLoggedIn,
                        userRepo = userRepo,
                        userService = userService,
                    )
                }
            Route.Auth ->
                NavEntry(key) {
                    val vm = viewModel<AuthViewModel>(factory = AuthViewModel.getFactory(userService))
                    AuthScreen(
                        onBack = onBack,
                        onAuthSuccess = onAuth,
                        vm = vm,
                    )
                }
            Route.Profile ->
                NavEntry(key) {
                    val vm = viewModel<UserProfileViewModel>(factory = UserProfileViewModel.getFactory(userService, userRepo))
                    UserProfileScreen(
                        onBack = onBack,
                        vm = vm,
                        userRepo = userRepo,
                        settingsVm = settingsVm,
                    )
                }
            Route.Main ->
                NavEntry(key) {
                    val mapVm =
                        viewModel<MapViewModel>(
                            factory = MapViewModel.getFactory(trailService),
                        )
                    MapScreen(
                        mapVm,
                        onUserProfile,
                        onTrails,
                        onSettings,
                        onLogout,
                        settingsVm,
                    )
                }
            Route.Trails ->
                NavEntry(key) {
                    val trailVm =
                        viewModel<TrailFetchViewModel>(
                            factory = TrailFetchViewModel.getFactory(trailService),
                        )
                    TrailsScreen(
                        trailVm,
                        onBack = onBack,
                        onStart = onHike,
                        settingsVm = settingsVm,
                    )
                }
            Route.WaitingRoom ->
                NavEntry(key) {
                    TODO()
                }
            is Route.Hike ->
                NavEntry(key) {
                    val hikeVm =
                        viewModel<TestHikingViewModel>(
                            factory = TestHikingViewModel.getFactory(hikeService, key.trail),
                        )
                    TestHikingScreen(hikeVm, onHikeStopped)
                }
            Route.Settings ->
                NavEntry(key) {
                    SettingsScreen(onBack, onLogout, onUserDelete, settingsVm)
                }
        }
    }
