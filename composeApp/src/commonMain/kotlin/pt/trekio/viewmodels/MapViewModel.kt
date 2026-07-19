package pt.trekio.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.touchlab.kermit.Logger
import io.github.tiagopraia.kmp.mapbox.GeographicPoint
import io.github.tiagopraia.kmp.mapbox.configs.CircleOverlay
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapOverlays
import io.github.tiagopraia.kmp.mapbox.configs.PolylineOverlay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.trekio.misc.ColorPalette
import pt.trekio.misc.Either
import pt.trekio.misc.Failure
import pt.trekio.misc.Success
import pt.trekio.misc.toGeoPoint
import pt.trekio.services.trails.TrailService
import pt.trekio.viewmodels.states.TrailState
import kotlin.collections.zipWithNext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MapViewModel(
    private val trailService: TrailService,
) : ViewModel() {
    companion object {
        fun getFactory(trailService: TrailService) =
            viewModelFactory {
                initializer { MapViewModel(trailService) }
            }
    }

    private fun <T> updateStateAfter(
        debugAction: String,
        operation: suspend () -> Either<String, T>,
        expectedStateFactory: (T) -> TrailState,
    ) {
        _state.value = TrailState.Loading
        viewModelScope.launch {
            _state.value =
                when (val res = operation()) {
                    is Failure -> {
                        Logger.e(tag = "MapViewModel") { "User $debugAction failure: ${res.message}" }
                        TrailState.Error(res.message)
                    }

                    is Success -> {
                        Logger.i(tag = "MapViewModel") { "User $debugAction succeeded" }
                        expectedStateFactory(res.value)
                    }
                }
        }
    }

    private fun createTrail(name: String) {
        val completed = _draftPoints.toList()
        val points = completed.map(GeographicPoint::toGeoPoint)
        updateStateAfter("trail creation", {
            trailService.createTrail(
                name,
                points.first(),
                points.last(),
                if (points.size >= 3) points.subList(1, points.size - 1) else emptyList(),
                null,
            )
        }) {
            val saved =
                _savedRoutes.add(
                    SavedRoute(
                        localId = draftRouteId,
                        serverId = it.id,
                        name = name,
                        points = completed,
                        isChild = false,
                        color = draftColor,
                    ),
                )
            if (!saved) TrailState.Error("Failed to save trail")
            _draftPoints.clear()
            isDrawingMode = false
            draftRouteName = ""
            hasCompleted = false
            TrailState.Created(it)
        }
    }

    private val _state by lazy {
        MutableStateFlow<TrailState>(TrailState.Idle)
    }
    val state = _state.asStateFlow()

    private val _savedRoutes = mutableStateListOf<SavedRoute>()
    val savedRoutes: List<SavedRoute> get() = _savedRoutes

    private val _draftPoints = mutableStateListOf<GeographicPoint>()
    val draftPoints: List<GeographicPoint> get() = _draftPoints

    @OptIn(ExperimentalUuidApi::class)
    var draftRouteId by mutableStateOf(Uuid.random().toString())
        private set

    var isDrawingMode by mutableStateOf(false)
        private set

    var hasCompleted by mutableStateOf(false)
        private set

    var draftRouteName by mutableStateOf("")
        private set

    var selection by mutableStateOf<Selection?>(null)
        private set

    private var requestedOverlayId: String? = null

    val canUndo: Boolean get() = _draftPoints.isNotEmpty()
    val canComplete: Boolean get() = _draftPoints.size >= 2

    val draftColor: ColorPalette
        get() = ColorPalette.entries[_savedRoutes.size % ColorPalette.entries.size]

    @OptIn(ExperimentalUuidApi::class)
    fun startNewRoute() {
        draftRouteId = Uuid.random().toString()
        isDrawingMode = true
    }

    fun addPoint(point: GeographicPoint) {
        _draftPoints.add(point)
    }

    fun undoLast() {
        if (_draftPoints.isNotEmpty()) _draftPoints.removeAt(_draftPoints.lastIndex)
    }

    fun completeRoute() {
        if (!canComplete) return
        hasCompleted = true
    }

    fun updateDraftRouteName(value: String) {
        draftRouteName = value
    }

    fun cancelRoute() {
        if (hasCompleted) {
            hasCompleted = false
            draftRouteName = ""
            _state.value = TrailState.Idle
        } else {
            val cancelDrawMode = !canUndo
            _draftPoints.clear()
            if (cancelDrawMode) isDrawingMode = false
        }
    }

    fun commitRoute() {
        if (!hasCompleted || !canComplete) return

        val name = draftRouteName.trim()
        if (name.isEmpty()) {
            _state.value = TrailState.Error("Write a name")
            return
        }

        createTrail(name)
    }

    fun buildOverlays(config: MapConfig): MapOverlays {
        val circles = mutableListOf<CircleOverlay>()
        val polylines = mutableListOf<PolylineOverlay>()

        _savedRoutes.forEach { route ->
            val hex = route.color.hex
            route.points.forEachIndexed { index, point ->
                circles.add(
                    CircleOverlay(
                        id = "${route.localId}_point_$index",
                        center = point,
                        radius = config.pointRadius,
                        colorHex = hex,
                    ),
                )
            }
            route.points.zipWithNext().forEachIndexed { index, (from, to) ->
                polylines.add(
                    PolylineOverlay(
                        id = "${route.localId}_segment_$index",
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
                        id = "${draftRouteId}_point_$index",
                        center = point,
                        radius = config.pointRadius,
                        colorHex = hex,
                    ),
                )
            }
            _draftPoints.zipWithNext().forEachIndexed { index, (from, to) ->
                polylines.add(
                    PolylineOverlay(
                        id = "${draftRouteId}_segment_$index",
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

    fun overlayClick(
        id: String,
        point: GeographicPoint,
    ) {
        Logger.i("Overlay clicado (localId): $id")
        val routeId = id.split("_", limit = 2).first()
        if (isDrawingMode) {
            Logger.i("Drawing Mode on, overlay click on ($routeId) ignored")
            return
        }

        selection = Selection(id, point)
        requestedOverlayId = id

        val route = _savedRoutes.firstOrNull { it.localId == routeId } ?: return
        val serverId = route.serverId ?: 0uL
        Logger.i("Overlay clicado (serverId): $serverId")

        updateStateAfter("trail details", {
            trailService.getTrailDetails(serverId)
        }) { trail ->
            if (requestedOverlayId == id) {
                TrailState.Details(trail)
            } else {
                _state.value
            }
        }
    }

    fun clearSelection() {
        selection = null
    }
}

data class SavedRoute
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        val localId: String,
        val serverId: ULong? = null,
        val name: String,
        val points: List<GeographicPoint>,
        val isChild: Boolean, // true = tracejado
        val color: ColorPalette,
    )

data class Selection(
    val overlayId: String,
    val clickPoint: GeographicPoint,
)
