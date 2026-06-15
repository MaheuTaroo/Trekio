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
import pt.trekio.services.user.UserService

@Composable
fun App(userService: UserService) {
    MaterialTheme {
        val backStack = rememberSaveable { mutableStateListOf<Route>(Route.Title) }
        NavDisplay(
            backStack = backStack,
            onBack = backStack::removeLastOrNull,
            entryProvider =
                navigationEntryProvider(
                    onUserProfile = { backStack.add(Route.Profile) },
                    onBack = backStack::removeLastOrNull,
                    onTrailCreation = { backStack.add(Route.TrailCreation) },
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
                    userService = userService,
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
