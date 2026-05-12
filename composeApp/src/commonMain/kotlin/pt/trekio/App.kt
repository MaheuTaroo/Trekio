package pt.trekio

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.ui.NavDisplay
import pt.trekio.nav.NavigationEntryProvider
import pt.trekio.nav.Route
import pt.trekio.services.user.UserService

@Composable
fun App(userService: UserService) {
    MaterialTheme {
        val backStack = remember { mutableStateListOf<Route>(Route.Main) }
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider =
                NavigationEntryProvider(
                    onToRegister = { backStack.add(Route.SignUp) },
                    onToLogin = { backStack.add(Route.Login) },
                    onRegisterClick = { backStack.add(Route.Main) },
                    onMapTest = { backStack.add(Route.MapTest) },
                    onLoginClick = { backStack.add(Route.Main) },
                    onUserProfile = { backStack.add(Route.Profile) },
                    onBack = { backStack.removeLastOrNull() },
                    userService = userService,
                    onSettings = {},
                ),
        )
    }
}
