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

    @Serializable
    data object Main : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Trails : Route

    @Serializable
    data object TrailCreation : Route

    @Serializable
    data object WaitingRoom : Route

    @Serializable
    data object Hike : Route
}
