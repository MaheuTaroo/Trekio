package pt.trekio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import pt.trekio.nav.Route
import pt.trekio.nav.navigationEntryProvider
import pt.trekio.platform.customAppLocale
import pt.trekio.repos.SettingsRepository
import pt.trekio.repos.UserRepository
import pt.trekio.services.hikes.HikeService
import pt.trekio.services.trails.TrailService
import pt.trekio.services.user.UserService
import pt.trekio.ui.theme.TrekioAppTheme
import pt.trekio.viewmodels.SettingsViewModel
import kotlin.collections.removeLastOrNull

fun MutableList<Route>.reset() {
    clear()
    add(Route.Title)
}

@Composable
fun App(
    userService: UserService,
    trailService: TrailService,
    hikeService: HikeService,
    userRepo: UserRepository,
) {
    val settingsRepo = SettingsRepository()
    val settingsVm =
        viewModel<SettingsViewModel>(
            factory = SettingsViewModel.getFactory(settingsRepo, userService),
        )
    customAppLocale = settingsRepo.getLanguage().tag
    val theme by settingsVm.theme.collectAsState()

    TrekioAppTheme(themeMode = theme) {
        val backStack = rememberSaveable { mutableStateListOf<Route>(Route.Title) }
        NavDisplay(
            backStack = backStack,
            onBack = {
                backStack.removeLastOrNull()
                if (backStack.isEmpty()) {
                    backStack.add(Route.Title)
                }
            },
            entryProvider =
                navigationEntryProvider(
                    userService = userService,
                    trailService = trailService,
                    hikeService = hikeService,
                    onUserProfile = { backStack.add(Route.Profile) },
                    onBack = backStack::removeLastOrNull,
                    onTrails = { backStack.add(Route.Trails) },
                    onToAuthenticate = { backStack.add(Route.Auth) },
                    onAuth = {
                        backStack.clear()
                        backStack.add(Route.Main)
                    },
                    onUserDelete = backStack::reset,
                    onLogout = backStack::reset,
                    onHike = { backStack.add(Route.Hike(it)) },
                    onHikeStopped = {
                        backStack.reset()
                        backStack.add(Route.Main)
                        backStack.add(Route.Profile)
                    },
                    onSettings = { backStack.add(Route.Settings) },
                    settingsVm = settingsVm,
                    onLoggedIn = {
                        backStack.clear()
                        backStack.add(Route.Main)
                    },
                    userRepo = userRepo,
                ),
            entryDecorators =
                listOf(
                    // Default decorator for navigation state
                    rememberSaveableStateHolderNavEntryDecorator(),
                    // Decorator for view model destruction on stack pop
                    rememberViewModelStoreNavEntryDecorator(),
                ),
        )
    }
}
