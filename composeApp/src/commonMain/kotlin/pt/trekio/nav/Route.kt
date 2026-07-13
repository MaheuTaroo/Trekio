package pt.trekio.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import pt.trekio.dto.TrailDto

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

    @Serializable
    data object Trails : Route

    @Serializable
    data object WaitingRoom : Route

    @Serializable
    data class Hike(
        val trail: TrailDto,
    ) : Route

    @Serializable
    data object Settings : Route
}
