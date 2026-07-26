package pt.trekio.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.tiagopraia.kmp.mapbox.GeographicPoint
import io.github.tiagopraia.kmp.mapbox.config.AndroidMapConfig
import io.github.tiagopraia.kmp.mapbox.configs.CircleOverlay
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapOverlays
import io.github.tiagopraia.kmp.mapbox.configs.MapStyle
import io.github.tiagopraia.kmp.mapbox.configs.PolylineOverlay
import io.github.tiagopraia.kmp.mapbox.map.AndroidMapWrapper
import pt.trekio.BuildKonfig
import pt.trekio.misc.ColorPalette

@Composable
actual fun MiniMapScreen(
    path: List<GeographicPoint>,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier =
                Modifier
                    .size(320.dp, 400.dp)
                    .clip(RoundedCornerShape(16.dp)),
        ) {
            AndroidMapWrapper(
                accessToken = BuildKonfig.MAPBOX_ACCESS_TOKEN,
                config = remember { AndroidMapConfig(mapConfig = MapConfig(styleUri = MapStyle.OUTDOORS)) },
                overlays =
                    remember(path) {
                        MapOverlays(
                            circles =
                                path.mapIndexed { i, p ->
                                    CircleOverlay(
                                        id = "preview_$i",
                                        center = p,
                                        radius = 5.0,
                                        colorHex = ColorPalette.BLUE.hex,
                                        isClickable = false,
                                    )
                                },
                            polylines =
                                listOf(
                                    PolylineOverlay(
                                        id = "preview_line",
                                        points = path,
                                        colorHex = ColorPalette.BLUE.hex,
                                        isClickable = false,
                                    ),
                                ),
                        )
                    },
                focusPoints = path,
                onLocationUpdate = { },
                onMapReady = { },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
