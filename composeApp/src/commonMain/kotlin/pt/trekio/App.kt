package pt.trekio

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import pt.trekio.nav.Route
import pt.trekio.nav.navigationEntryProvider
import pt.trekio.services.hikes.HikeService
import pt.trekio.services.trails.TrailService
import pt.trekio.services.user.UserService

@Composable
fun App(
    userService: UserService,
    trailService: TrailService,
    hikeService: HikeService,
) {
    MaterialTheme {
        val backStack = rememberSaveable { mutableStateListOf<Route>(Route.Title) }
        NavDisplay(
            backStack = backStack,
            onBack = backStack::removeLastOrNull,
            entryProvider =
                navigationEntryProvider(
                    userService = userService,
                    trailService = trailService,
                    hikeService = hikeService,
                    onUserProfile = { backStack.add(Route.Profile) },
                    onBack = backStack::removeLastOrNull,
                    onTrails = { backStack.add(Route.Trails) },
                    onToAuthenticate = { backStack.add(Route.Auth) },
                    onAuth = { backStack.add(Route.Main) },
                    onUserDelete = {
                        backStack.clear()
                        backStack.add(Route.Title)
                    },
                    onLogout = {
                        backStack.clear()
                        backStack.add(Route.Title)
                    },
                    onHike = {
                        backStack.add(Route.Hike(it))
                    }
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
