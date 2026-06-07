package pt.trekio.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.tiagopraia.kmp.mapbox.GeoPoint
import io.github.tiagopraia.kmp.mapbox.configs.CircleOverlay
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapOverlays
import io.github.tiagopraia.kmp.mapbox.configs.PolylineOverlay
import kotlin.collections.zipWithNext

private const val CHILD_ROUTE_THRESHOLD_METERS = 25.0

class MapViewModel : ViewModel() {
    companion object {
        fun getFactory() =
            viewModelFactory {
                initializer { MapViewModel() }
            }
    }

    private val _savedRoutes = mutableStateListOf<SavedRoute>()
    val savedRoutes: List<SavedRoute> get() = _savedRoutes

    private val _draftPoints = mutableStateListOf<GeoPoint>()
    val draftPoints: List<GeoPoint> get() = _draftPoints

    var draftRouteId by mutableIntStateOf(0)
        private set

    var isDrawingMode by mutableStateOf(false)
        private set

    val canUndo: Boolean get() = _draftPoints.isNotEmpty()
    val canComplete: Boolean get() = _draftPoints.size >= 2

    val draftColor: ColorPalette
        get() = ColorPalette.entries[_savedRoutes.size % ColorPalette.entries.size]

    fun startNewRoute() {
        draftRouteId++
        isDrawingMode = true
    }

    fun addPoint(point: GeoPoint) {
        _draftPoints.add(point)
    }

    fun undoLast() {
        if (_draftPoints.isNotEmpty()) _draftPoints.removeAt(_draftPoints.lastIndex)
    }

    fun completeRoute() {
        if (!canComplete) return
        val completed = _draftPoints.toList()
        val isChild =
            _savedRoutes.any { existing ->
                existing.points.any { existingPoint ->
                    completed.any { newPoint ->
                        false // existingPoint.distanceTo(newPoint) < CHILD_ROUTE_THRESHOLD_METERS
                    }
                }
            }
        _savedRoutes.add(
            SavedRoute(
                id = "route-${_savedRoutes.size}",
                points = completed,
                isChild = isChild,
                color = draftColor,
            ),
        )
        _draftPoints.clear()
        isDrawingMode = false
    }

    fun cancelRoute() {
        val cancelDrawMode = !canUndo
        _draftPoints.clear()
        if (cancelDrawMode) isDrawingMode = false
    }

    fun buildOverlays(config: MapConfig): MapOverlays {
        val circles = mutableListOf<CircleOverlay>()
        val polylines = mutableListOf<PolylineOverlay>()

        _savedRoutes.forEach { route ->
            val hex = route.color.hex
            route.points.forEachIndexed { index, point ->
                circles.add(
                    CircleOverlay(
                        id = "${route.id}-point-$index",
                        center = point,
                        radius = config.pointRadius,
                        colorHex = hex,
                    ),
                )
            }
            route.points.zipWithNext().forEachIndexed { index, (from, to) ->
                polylines.add(
                    PolylineOverlay(
                        id = "${route.id}-segment-$index",
                        points = listOf(from, to),
                        colorHex = hex,
                        width = config.lineWidth,
                        isDashed = route.isChild,
                    ),
                )
            }
        }

        if (_draftPoints.isNotEmpty()) {
            val hex = draftColor.hex
            _draftPoints.forEachIndexed { index, point ->
                circles.add(
                    CircleOverlay(
                        id = "draft-$draftRouteId-point-$index",
                        center = point,
                        radius = config.pointRadius,
                        colorHex = hex,
                    ),
                )
            }
            _draftPoints.zipWithNext().forEachIndexed { index, (from, to) ->
                polylines.add(
                    PolylineOverlay(
                        id = "draft-$draftRouteId-segment-$index",
                        points = listOf(from, to),
                        colorHex = hex,
                        width = config.lineWidth,
                        isDashed = false,
                    ),
                )
            }
        }

        return MapOverlays(circles = circles, polylines = polylines)
    }
}

data class SavedRoute(
    val id: String,
    val points: List<GeoPoint>,
    val isChild: Boolean, // true = tracejado
    val color: ColorPalette,
)

enum class ColorPalette(
    val hex: String,
) {
    RED("#E53935"),
    BLUE("#1E88E5"),
    GREEN("#43A047"),
    YELLOW("#FFB300"),
    PURPLE("#8E24AA"),
    CIAN("#00ACC1"),
    PINK("#E91E63"),
    TEAL("#00897B"),
    ORANGE("#FF7043"),
    VIOLET("#5E35B1"),
    LILAC("#727CFF"),
    GREEN_LIME("#7CB342"),
    RED_ORANGE("#F4511E"),
    INDIGO("#3949AB"),
    WATER_GREEN("#00BFA5"),
    MAGENTA("#D81B60"),
    BROWN("#6D4C41"),
    GREY("#546E7A"),
    LIME("#C0CA33"),
    DARK_ORANGE("#EF6C00"),
}
