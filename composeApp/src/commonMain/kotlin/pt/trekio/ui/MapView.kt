package pt.trekio.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import dev.jordond.compass.Coordinates
import dev.jordond.compass.Location
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.mobile
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
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
) {
    val state = rememberCameraState(CameraPosition(zoom = 5.0))
    var currentAccuracy by remember { mutableDoubleStateOf(1.0) }
    MaplibreMap(
        cameraState = state,
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        modifier = modifier,
    ) {
        var src = rememberGeoJsonSource(
            data = GeoJsonData.Features(
                Point(
                    Position(0.0, 0.0, 0.0)
                )
            )
        )

        for (trail in trails) {
            val start = trail.value.coordinates.first()
            val end = trail.value.coordinates.last()

            PointPuck(
                "start",
                GeoSource(
                    "start",
                    Point(
                    Position(start[0], start[1], start[2])
                    )
                ),
                Color.Green
            )
            LineLayer(
                trail.key,
                GeoSource(trail.key, trail.value)
            )
            PointPuck(
                "finish",
                GeoSource(
                    "finish",
                    Point(
                        Position(end[0], end[1], end[2])
                    )
                ),
                Color.Blue
            )
        }

        if (trackUser) {
            val locator = remember { Geolocator.mobile() }
            val scope = rememberCoroutineScope()
            val state = locator.locationUpdates.collectAsState(
                    Location(
                        Coordinates(.0, .0),
                        1.0,
                        null,
                        null,
                        null,
                        null,
                        0L
                    )
                )
            }
            LaunchedEffect(Unit) {
                Logger.i { "Awaiting start from track..." }
                Logger.i { "${locator.startTracking()} from tracking" }
            }

            LocationPuck(
                src,
                currentAccuracy,
                state.metersPerDpAtTarget
            )
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