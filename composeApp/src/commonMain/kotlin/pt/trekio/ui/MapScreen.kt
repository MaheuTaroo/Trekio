package pt.trekio.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import dev.jordond.compass.Location
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.painterResource
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.GeoJsonSource
import org.maplibre.compose.sources.Source
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.GeoJsonObject
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import pt.trekio.viewmodels.MapScreenViewModel
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.crosshair

private const val LOC_DELTA = .0000001

private val defaultOptions = GeoJsonOptions()

private val CENTERING_BTN_OUTER_PADDING = 15.dp
private const val CENTERING_BTN_INNER_PADDING_FACTOR = 35

fun defaultSource(
    id: String,
    data: GeoJsonObject,
) = GeoJsonSource(id, GeoJsonData.Features(data), defaultOptions)

fun CameraPosition.isEqualTo(location: Location) =
    target.latitude - location.coordinates.latitude < LOC_DELTA &&
        target.longitude - location.coordinates.longitude < LOC_DELTA

@Composable
private fun MapContainer(
    trails: Map<String, LineString>,
    cameraState: CameraState,
    isUsingDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit),
) {
    MaplibreMap(
        cameraState = cameraState,
        baseStyle =
            BaseStyle.Uri(
                "https://tiles.openfreemap.org/styles/" + if (isUsingDarkTheme) "dark" else "liberty",
            ),
        modifier = modifier,
    ) {
        for (trail in trails) {
            val start = trail.value.coordinates.first()
            val end = trail.value.coordinates.last()

            PointPuck(
                "start-${trail.key}",
                defaultSource(
                    "start-${trail.key}",
                    Point(
                        Position(
                            start[0],
                            start[1],
                            start[2],
                        ),
                    ),
                ),
                Color.Green,
            )
            LineLayer(
                trail.key,
                defaultSource(trail.key, trail.value),
            )
            PointPuck(
                "finish-${trail.key}",
                defaultSource(
                    "finish-${trail.key}",
                    Point(
                        Position(
                            end[0],
                            end[1],
                            end[2],
                        ),
                    ),
                ),
                Color.Blue,
            )
        }

        content()
    }
}

@Composable
fun MapScreen(
    vm: MapScreenViewModel,
    isUsingDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val cameraState =
        rememberCameraState(
            CameraPosition(zoom = if (vm.trackingUser) 17.5 else 1.5),
        )
    var prevZoom by remember { mutableDoubleStateOf(cameraState.position.zoom) }
    val currentLocation by vm.coordinates.collectAsState(null)
    val mutex = remember { Mutex() }
    var lastMoveWasGesture by remember { mutableStateOf(false) }

    // Hoisted out of the content lambda — no longer gated on isCameraMoving,
    // so swapping the entire lambda on every camera move event is avoided.
    if (vm.trackingUser) {
        LaunchedEffect(currentLocation, vm.shouldTrack) {
            mutex.withLock {
                if (currentLocation != null && vm.shouldTrack) {
                    cameraState.animateTo(
                        cameraState.position.copy(
                            target =
                                Position(
                                    currentLocation!!.coordinates.longitude,
                                    currentLocation!!.coordinates.latitude,
                                ),
                            zoom = prevZoom,
                        ),
                    )
                }
            }
        }

        LaunchedEffect(cameraState.isCameraMoving) {
            mutex.withLock {
                when {
                    cameraState.isCameraMoving -> {
                        if (cameraState.moveReason == CameraMoveReason.GESTURE) {
                            vm.shouldTrack = false
                            lastMoveWasGesture = true
                            prevZoom = cameraState.position.zoom
                        } else if (lastMoveWasGesture) {
                            lastMoveWasGesture = false
                        }
                    }

                    lastMoveWasGesture -> prevZoom = cameraState.position.zoom

                    else -> {
                        val diff = prevZoom - cameraState.position.zoom
                        if (diff > LOC_DELTA) {
                            cameraState.animateTo(cameraState.position.copy(zoom = prevZoom))
                        }
                    }
                }
            }
        }
    }

    Column {
        MapContainer(
            trails = vm.trails,
            cameraState = cameraState,
            isUsingDarkTheme = isUsingDarkTheme,
            modifier = modifier,
        ) {
            // Content is now always stable — no lambda swap, no isCameraMoving gate.
            if (vm.trackingUser) {
                currentLocation?.let { loc ->
                    // rememberGeoJsonSource handles reactive data updates internally;
                    // key() was forcing full layer teardown/rebuild on every GPS tick.
                    val src =
                        rememberGeoJsonSource(
                            GeoJsonData.Features(
                                Point(
                                    Position(
                                        loc.coordinates.longitude,
                                        loc.coordinates.latitude,
                                        loc.ellipsoidalAltitude?.meters ?: 0.0,
                                    ),
                                ),
                            ),
                            defaultOptions,
                        )
                    LocationPuck(src, loc.accuracy, cameraState.metersPerDpAtTarget)
                }
            }
        }
    }

    if (vm.trackingUser && !vm.shouldTrack) {
        val trackingCoroutineScope = rememberCoroutineScope()
        currentLocation?.let {
            if (!cameraState.position.isEqualTo(it)) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    FloatingActionButton(
                        modifier =
                            Modifier
                                .padding(bottom = CENTERING_BTN_OUTER_PADDING, end = CENTERING_BTN_OUTER_PADDING)
                                .size(LocalWindowInfo.current.containerDpSize.width / 5),
                        shape = CircleShape,
                        onClick = {
                            trackingCoroutineScope.launch {
                                cameraState.animateTo(
                                    cameraState.position.copy(
                                        target =
                                            Position(
                                                currentLocation!!.coordinates.longitude,
                                                currentLocation!!.coordinates.latitude,
                                            ),
                                        zoom = prevZoom,
                                    ),
                                )
                            }
                            vm.shouldTrack = true
                        },
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.crosshair),
                            contentDescription = "Center location",
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(
                                        LocalWindowInfo.current.containerDpSize.width / CENTERING_BTN_INNER_PADDING_FACTOR,
                                    ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PointPuck(
    id: String,
    location: Source,
    color: Color = Color.Black,
) {
    CircleLayer(
        id = id,
        source = location,
        color = const(color),
    )
}

@Composable
private fun LocationPuck(
    locationSource: Source,
    accuracy: Double,
    metersPerDp: Double,
) {
    CircleLayer(
        id = "location-accuracy",
        visible = true,
        source = locationSource,
        radius = const((accuracy / metersPerDp).dp),
        opacity = const(.5f),
        color = const(Color.Cyan),
        strokeColor = const(Color.White),
        strokeWidth = const(1.dp),
    )
    CircleLayer(
        id = "location-puck-shadow",
        source = locationSource,
        radius = const(12.dp),
        blur = const(.5f),
        color = const(Color.LightGray),
    )
    CircleLayer(
        id = "location-puck",
        source = locationSource,
        radius = const(5.dp),
        strokeWidth = const(3.dp),
        strokeColor = const(Color.White),
        color = const(Color.Blue),
    )
}
