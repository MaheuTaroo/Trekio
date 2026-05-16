package pt.trekio.nav

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import pt.trekio.services.user.UserService
import pt.trekio.ui.LoginScreen
import pt.trekio.ui.MainScreen
import pt.trekio.ui.MapScreen
import pt.trekio.ui.SignUpScreen
import pt.trekio.ui.TitleScreen
import pt.trekio.ui.UserProfileScreen
import pt.trekio.viewmodels.LoginViewModel
import pt.trekio.viewmodels.MapScreenViewModel
import pt.trekio.viewmodels.SignUpViewModel
import pt.trekio.viewmodels.UserProfileViewModel

fun NavigationEntryProvider(
    onToRegister: () -> Unit,
    onToLogin: () -> Unit,
    onMapTest: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    onUserProfile: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    userService: UserService,
): (Route) -> NavEntry<Route> =
    { key ->
        when (key) {
            Route.Title ->
                NavEntry(key) {
                    TitleScreen(
                        onRegisterClick = onToRegister,
                        onLoginClick = onToLogin,
                    )
                }
            Route.SignUp ->
                NavEntry(key) {
                    val vm = viewModel<SignUpViewModel>(factory = SignUpViewModel.getFactory(userService))
                    SignUpScreen(
                        onBack = onBack,
                        onSignUp = onRegisterClick,
                        onGoogleSignUp = { /* TODO */ },
                        vm = vm,
                    )
                }
            Route.Login ->
                NavEntry(key) {
                    val vm = viewModel<LoginViewModel>(factory = LoginViewModel.getFactory(userService))
                    LoginScreen(
                        onBack = onBack,
                        onLogin = onLoginClick,
                        onGoogleLogin = { /* TODO */ },
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
                        viewModel<MapScreenViewModel>(
                            key = "map_main",
                            factory = MapScreenViewModel.getFactory(trackUser = false),
                        )
                    MainScreen(
                        onUserProfile,
                        onSettings,
                        onMapTest,
                        {},
                        mapVm,
                    )
                }

            Route.MapTest -> {
                NavEntry(key) {
                    val vm =
                        viewModel<MapScreenViewModel>(
                            key = "mapTest",
                            factory = MapScreenViewModel.getFactory(trackUser = true),
                        )
                    MapScreen(vm)
                }
            }
            Route.Settings ->
                NavEntry(key) {
                    TODO()
                }
            Route.Trails ->
                NavEntry(key) {
                    TODO()
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
