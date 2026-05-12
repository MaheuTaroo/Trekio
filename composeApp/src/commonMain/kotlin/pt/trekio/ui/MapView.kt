package pt.trekio.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
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
internal fun LocalizationEnforcer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier) {
        content()
    }
}

@Composable
fun MapView(
    coordinates: Flow<Point>,
    trails: Map<String, LineString> = mapOf(),
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val state = rememberCameraState(CameraPosition(zoom = 5.0))
    MaplibreMap(
        cameraState = state,
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        modifier = modifier,
    ) {
        val src = rememberGeoJsonSource(
            data = GeoJsonData.Features(
                Point(
                    Position(0.0, 0.0, 0.0)
                )
            )
        )
        LaunchedEffect(Unit) {
            coordinates.collect {
                println("New data: $it")
                src.setData(GeoJsonData.Features(it))
                state.position = CameraPosition(
                    target = it.coordinates,
                    zoom = state.position.zoom
                )
            }
        }

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
                        Position(start[0], start[1], start[2])
                    )
                ),
                Color.Blue
            )
        }

        LocationPuck(
            src,
            .5,
            10.0
        )
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