package pt.trekio.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Title : Route

    @Serializable
    data object Login : Route

    @Serializable
    data object SignUp : Route

    @Serializable
    data object Profile : Route
}
