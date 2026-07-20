package pt.trekio.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import pt.trekio.dto.TrailDto

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Title : Route

    @Serializable
    data class Auth(
        val username: String? = null,
        val new: Boolean? = null,
        val error: String? = null,
    ) : Route

    @Serializable
    data object Main : Route

    @Serializable
    data object Profile : Route

    @Serializable
    data object Trails : Route

    @Serializable
    data class Hike(
        val trail: TrailDto,
    ) : Route

    @Serializable
    data object Settings : Route
}
