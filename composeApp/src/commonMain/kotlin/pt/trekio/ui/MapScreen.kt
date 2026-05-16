package pt.trekio.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
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

private val defaultOptions = GeoJsonOptions()

private fun GeoSource(
    id: String,
    data: GeoJsonObject,
) = GeoJsonSource(id, GeoJsonData.Features(data), defaultOptions)

@Composable
private fun MapContainer(
    trails: Map<String, LineString>,
    cameraState: CameraState,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit),
) {
    MaplibreMap(
        cameraState = cameraState,
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        modifier = modifier,
    ) {
        for (trail in trails) {
            val start = trail.value.coordinates.first()
            val end = trail.value.coordinates.last()

            PointPuck(
                "start-${trail.key}",
                GeoSource(
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
                GeoSource(trail.key, trail.value),
            )
            PointPuck(
                "finish-${trail.key}",
                GeoSource(
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
    modifier: Modifier = Modifier,
) {
    val cameraState = rememberCameraState(
        CameraPosition(
            zoom = if (vm.trackingUser) 17.5 else 1.5
        )
    )
    var prevZoom by remember { mutableDoubleStateOf(cameraState.position.zoom) }

    Logger.i { "vm@${vm.hashCode()}: Tracking is ${vm.trackingUser}, henceforth zoom is ${cameraState.position.zoom}" }

    val composable: @Composable (() -> Unit) =
        if (vm.trackingUser) {
            content@{
                val currentLocation by vm.coordinates.collectAsState(null)

                LaunchedEffect(currentLocation) {
                    if (currentLocation != null) {
                        cameraState.animateTo(
                            cameraState.position.copy(
                                target = Position(
                                    currentLocation!!.coordinates.longitude,
                                    currentLocation!!.coordinates.latitude,
                                ),
                                zoom = prevZoom
                            ),
                        )
                    }
                }

                LaunchedEffect(cameraState.isCameraMoving, cameraState.position.zoom) {
                    if (cameraState.isCameraMoving) {
                        Logger.i {
                            "Camera moving, reason: ${cameraState.moveReason}"
                        }
                        if (cameraState.moveReason == CameraMoveReason.GESTURE) {
                            prevZoom = cameraState.position.zoom
                            Logger.i {
                                "Since reason is gesture, previous zoom set to $prevZoom"
                            }
                        }
                    }
                    else {
                        val diff = prevZoom - cameraState.position.zoom
                        Logger.i {
                            "Not moving anymore; " +
                                    "previous zoom is $prevZoom, current is different by $diff"
                        }
                        if (diff > 0.0000001) {
                            cameraState.animateTo(cameraState.position.copy(zoom = prevZoom))
                            Logger.i {
                                "Reason is non-gesture; zoom adjusted"
                            }
                        }
                    }
                }

                currentLocation?.let {
                    key(it.coordinates) {
                        val src = rememberGeoJsonSource(
                            data = GeoJsonData.Features(
                                Point(
                                    Position(
                                        it.coordinates.longitude,
                                        it.coordinates.latitude,
                                        it.ellipsoidalAltitude?.meters ?: 0.0,
                                    ),
                                ),
                            )
                        )

                        LocationPuck(src, it.accuracy, cameraState.metersPerDpAtTarget)
                    }
                }
            }
        } else {
            { }
        }

    MapContainer(vm.trails, cameraState, modifier, composable)
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
