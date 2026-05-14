package pt.trekio.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Title : Route
    @Serializable
    data object Auth : Route
    @Serializable
    data object Main : Route
    @Serializable
    data object Profile : Route
}
