package pt.trekio.nav

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import io.github.tiagopraia.kmp.mapbox.MapViewModel
import pt.trekio.services.user.UserService
import pt.trekio.ui.AuthScreen
import pt.trekio.ui.MapScreen
import pt.trekio.ui.TitleScreen
import pt.trekio.ui.TrailsScreen
import pt.trekio.ui.UserProfileScreen
import pt.trekio.viewmodels.AuthViewModel
import pt.trekio.viewmodels.MapViewModel
import pt.trekio.viewmodels.UserProfileViewModel

fun NavigationEntryProvider(
    onUserProfile: () -> Unit,
    onBack: () -> Unit,
    onTrailCreation: () -> Unit,
    onTrails: () -> Unit,
    onToAuthenticate: () -> Unit,
    onAuth: () -> Unit,
    userService: UserService,
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
                        onDelete = { /* TODO: delete profile */ },
                        vm = vm,
                    )
                }
            Route.Main ->
                NavEntry(key) {
                    val mapVm =
                        viewModel<MapViewModel>(
                            factory = MapViewModel.getFactory(),
                        )
                    MapScreen(
                        mapVm,
                        onUserProfile,
                        onTrails,
                    )
                }
            Route.Trails ->
                NavEntry(key) {
                    TrailsScreen(
                        onBack = onBack,
                        onTrailCreation = onTrailCreation,
                        onStart = { },
                    )
                }
            Route.TrailCreation ->
                NavEntry(key) {
                    TODO()
                }
            Route.WaitingRoom ->
                NavEntry(key) {
                    TODO()
                }
            Route.Hike ->
                NavEntry(key) {
                    TODO()
                }
        }
    }
