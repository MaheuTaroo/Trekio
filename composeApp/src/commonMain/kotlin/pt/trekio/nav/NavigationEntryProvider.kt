package pt.trekio.nav

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import pt.trekio.dto.TrailDto
import pt.trekio.repos.SettingsRepository
import pt.trekio.repos.UserRepository
import pt.trekio.services.hikes.HikeService
import pt.trekio.services.trails.TrailService
import pt.trekio.services.user.UserService
import pt.trekio.ui.AuthScreen
import pt.trekio.ui.HikingScreen
import pt.trekio.ui.MapScreen
import pt.trekio.ui.SettingsScreen
import pt.trekio.ui.TitleScreen
import pt.trekio.ui.TrailsScreen
import pt.trekio.ui.UserProfileScreen
import pt.trekio.viewmodels.AuthViewModel
import pt.trekio.viewmodels.HikingViewModel
import pt.trekio.viewmodels.MapViewModel
import pt.trekio.viewmodels.SettingsViewModel
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
    settingsRepo: SettingsRepository,
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
            is Route.Auth ->
                NavEntry(key) {
                    val vm = viewModel<AuthViewModel>(factory = AuthViewModel.getFactory(userService))
                    AuthScreen(
                        onBack = onBack,
                        onAuthSuccess = onAuth,
                        vm = vm,
                        username = key.username,
                        new = key.new,
                        error = key.error,
                    )
                }
            Route.Profile ->
                NavEntry(key) {
                    val vm = viewModel<UserProfileViewModel>(factory = UserProfileViewModel.getFactory(userService, userRepo))
                    val settingsVm =
                        viewModel<SettingsViewModel>(
                            factory = SettingsViewModel.getFactory(settingsRepo, userService),
                        )
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
                    val settingsVm =
                        viewModel<SettingsViewModel>(
                            factory = SettingsViewModel.getFactory(settingsRepo, userService),
                        )
                    MapScreen(
                        mapVm,
                        onUserProfile,
                        onTrails,
                        onSettings,
                        onLogout,
                        onHike,
                        settingsVm,
                        userRepo,
                    )
                }
            Route.Trails ->
                NavEntry(key) {
                    val trailVm =
                        viewModel<TrailFetchViewModel>(
                            factory = TrailFetchViewModel.getFactory(trailService),
                        )
                    val settingsVm =
                        viewModel<SettingsViewModel>(
                            factory = SettingsViewModel.getFactory(settingsRepo, userService),
                        )
                    TrailsScreen(
                        trailVm,
                        onBack = onBack,
                        onStart = onHike,
                        settingsVm = settingsVm,
                    )
                }
            is Route.Hike ->
                NavEntry(key) {
                    val hikeVm =
                        viewModel<HikingViewModel>(
                            factory = HikingViewModel.getFactory(hikeService, key.trail),
                        )
                    val settingsVm =
                        viewModel<SettingsViewModel>(
                            factory = SettingsViewModel.getFactory(settingsRepo, userService),
                        )
                    HikingScreen(hikeVm, settingsVm, onHikeStopped)
                }
            Route.Settings ->
                NavEntry(key) {
                    val settingsVm =
                        viewModel<SettingsViewModel>(
                            factory = SettingsViewModel.getFactory(settingsRepo, userService),
                        )
                    SettingsScreen(
                        onBack,
                        onLogout,
                        onUserDelete,
                        settingsVm,
                        userRepo,
                    )
                }
        }
    }
