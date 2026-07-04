package pt.trekio.nav

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import pt.trekio.services.hikes.HikeService
import pt.trekio.services.trails.TrailService
import pt.trekio.services.user.UserService
import pt.trekio.ui.AuthScreen
import pt.trekio.ui.MapScreen
import pt.trekio.ui.TestHikingScreen
import pt.trekio.ui.TitleScreen
import pt.trekio.ui.TrailsScreen
import pt.trekio.ui.UserProfileScreen
import pt.trekio.viewmodels.AuthViewModel
import pt.trekio.viewmodels.MapViewModel
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
    onHike: (ULong) -> Unit,
): (Route) -> NavEntry<Route> =
    { key ->
        when (key) {
            Route.Title ->
                NavEntry(key) {
                    TitleScreen(
                        onAuthenticateClick = onToAuthenticate,
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
                    val vm = viewModel<UserProfileViewModel>(factory = UserProfileViewModel.getFactory(userService))
                    UserProfileScreen(
                        onBack = onBack,
                        onDelete = onUserDelete,
                        onLogout = onLogout,
                        vm = vm,
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
                        onStart = { onHike(it) },
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
                            factory = TestHikingViewModel.getFactory(hikeService, (key as Route.Hike).trailId),
                        )
                    TestHikingScreen(hikeVm)
                }
        }
    }
