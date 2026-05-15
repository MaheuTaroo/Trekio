package pt.trekio.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.jordond.compass.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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


private val defaultOptions = GeoJsonOptions()

private fun GeoSource(id: String, data: GeoJsonObject) =
    GeoJsonSource(id, GeoJsonData.Features(data), defaultOptions)

@Composable
fun MapView(
    modifier: Modifier = Modifier,
    trails: Map<String, LineString> = mapOf(),
    trackUser: Boolean = false,
    locationFlow: Flow<Location>? = null,
) {
    val cameraState = rememberCameraState(CameraPosition(zoom = 17.5))
    val currentLocation by (locationFlow ?: emptyFlow()).collectAsState(initial = null)

    LaunchedEffect(currentLocation) {
        if (currentLocation != null) {
            cameraState.animateTo(
                CameraPosition(
                    target = Position(
                        currentLocation!!.coordinates.longitude,
                        currentLocation!!.coordinates.latitude,
                    ),
                    zoom = cameraState.position.zoom
                )
            )
        }
    }

    MaplibreMap(
        cameraState = cameraState,
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        modifier = modifier,
    ) {
        for (trail in trails) {
            val start = trail.value.coordinates.first()
            val end = trail.value.coordinates.last()

            PointPuck(
                "start",
                GeoSource("start", Point(Position(start[0], start[1], start[2]))),
                Color.Green
            )
            LineLayer(
                trail.key,
                GeoSource(trail.key, trail.value)
            )
            PointPuck(
                "finish",
                GeoSource("finish", Point(Position(end[0], end[1], end[2]))),
                Color.Blue
            )
        }

        if (trackUser && locationFlow != null && currentLocation != null) {
            key(currentLocation!!.coordinates) {
                val locationSource = rememberGeoJsonSource(
                    data = GeoJsonData.Features(
                        Point(
                            Position(
                                currentLocation!!.coordinates.longitude,
                                currentLocation!!.coordinates.latitude,
                            )
                        )
                    )
                )
                LocationPuck(
                    locationSource = locationSource,
                    accuracy = currentLocation!!.accuracy,
                    metersPerDp = cameraState.metersPerDpAtTarget,
                )
            }
        }
    }
}

@Composable
private fun PointPuck(
    id: String,
    location: Source,
    color: Color = Color.Black
) {
    CircleLayer(
        id = id,
        source = location,
        color = const(color)
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