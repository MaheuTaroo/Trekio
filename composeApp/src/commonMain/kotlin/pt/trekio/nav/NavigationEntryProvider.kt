package pt.trekio.nav

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import pt.trekio.services.user.UserService
import pt.trekio.ui.AuthScreen
import pt.trekio.ui.MainScreen
import pt.trekio.ui.TitleScreen
import pt.trekio.ui.UserProfileScreen
import pt.trekio.viewmodels.AuthViewModel
import pt.trekio.viewmodels.UserProfileViewModel

fun NavigationEntryProvider(
    onToAuthenticate: () -> Unit,
    onAuth: () -> Unit,
    onBack: () -> Unit,
    onProfile: () -> Unit,
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
            Route.Main ->
                NavEntry(key) {
                    MainScreen(
                        onProfileClick = onProfile,
                        onSettingsClick = {},
                        onTrailsClick = {},
                    )
                }
            Route.Profile ->
                NavEntry(key) {
                    val vm = viewModel<UserProfileViewModel>(factory = UserProfileViewModel.getFactory(userService))
                    UserProfileScreen(
                        onBack = onBack,
                        onDelete = { /* TODO: save profile */ },
                        vm = vm,
                    )
                }
//            Route.MapTest -> {
//                NavEntry(key) {
//                    val vm =
//                        viewModel<MapScreenViewModel>(
//                            factory = MapScreenViewModel.getFactory(trackUser = true),
//                        )
//                    MapScreen(vm)
//                }
//            }
        }
    }
