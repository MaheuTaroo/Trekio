package pt.trekio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import io.github.tiagopraia.kmp.mapbox.AndroidMapWrapper
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapStyle
import pt.trekio.BuildKonfig
import pt.trekio.viewmodels.MapViewModel

@Composable
actual fun MapScreen(
    viewModel: MapViewModel,
    onProfileClick: () -> Unit,
    onTrailsClick: () -> Unit,
) {
    val theme = isSystemInDarkTheme()
    val config =
        remember {
            MapConfig(
                styleUri = if (theme) MapStyle.DARK else MapStyle.OUTDOORS,
            )
        }
    var mapReady by remember { mutableStateOf(false) }

    val overlays by remember(
        viewModel.savedRoutes,
        viewModel.draftPoints,
        viewModel.draftRouteId,
    ) {
        derivedStateOf { viewModel.buildOverlays(config) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidMapWrapper(
            accessToken = BuildKonfig.MAPBOX_ACCESS_TOKEN,
            overlays = overlays,
            config = config,
            onOverlayClick = { id ->
                Logger.i("Overlay clicado: $id")
            },
            onMapReady = { mapReady = true },
            onMapClick = { geoPoint ->
                if (viewModel.isDrawingMode) {
                    viewModel.addPoint(geoPoint)
                    true
                } else {
                    false
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (mapReady) {
            MapOverlayButtons(
                followButtonConfig = config.followButton,
                isDrawingMode = viewModel.isDrawingMode,
                canUndo = viewModel.canUndo,
                canComplete = viewModel.canComplete,
                onProfileClick = onProfileClick,
                onTrailsClick = onTrailsClick,
                onStartRoute = { viewModel.startNewRoute() },
                onUndo = { viewModel.undoLast() },
                onCancel = { viewModel.cancelRoute() },
                onComplete = { viewModel.completeRoute() },
            )
        }
    }
}
