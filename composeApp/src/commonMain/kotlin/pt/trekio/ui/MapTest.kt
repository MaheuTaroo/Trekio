package pt.trekio.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.Source
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.GeoJsonObject
import org.maplibre.spatialk.geojson.Point
import pt.trekio.viewmodels.MapTestViewModel

@Composable
fun MapTest(vm: MapTestViewModel) {
    val state = rememberCameraState(CameraPosition(zoom = 15.0))
    MaplibreMap(cameraState = state, baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty")) {
        val src = rememberGeoJsonSource(
            data = GeoJsonData.Features(
                Point.fromJson(
                    "{\"type\": \"Point\", \"coordinates\": [0.0, 0.0, 0.0]}"
                )
            )
        )
        LaunchedEffect(Unit) {
            vm.coordinates.collect {
                println("New data: $it")
                val data = GeoJsonObject.fromJson(it)
                src.setData(GeoJsonData.Features(data))
                state.position = CameraPosition(
                    target = (data as Point).coordinates,
                    zoom = state.position.zoom
                )
            }
        }

        LocationPuck(
            src,
            .5,
            10.0
        )
    }
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