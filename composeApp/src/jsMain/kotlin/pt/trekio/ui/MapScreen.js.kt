package pt.trekio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import io.github.tiagopraia.kmp.mapbox.WebMapConfig
import io.github.tiagopraia.kmp.mapbox.WebMapWrapper
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapStyle
import pt.trekio.BuildKonfig
import pt.trekio.viewmodels.MapViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun MapScreen(
    viewModel: MapViewModel,
    onProfileClick: () -> Unit,
    onTrailsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val theme = isSystemInDarkTheme()
    val config =
        remember {
            WebMapConfig(
                mapConfig =
                    MapConfig(
                        styleUri = if (theme) MapStyle.DARK else MapStyle.OUTDOORS,
                    ),
            )
        }

    val overlays by remember(
        viewModel.savedRoutes,
        viewModel.draftPoints,
        viewModel.draftRouteId,
    ) {
        derivedStateOf { viewModel.buildOverlays(config.mapConfig) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        WebMapWrapper(
            accessToken = BuildKonfig.MAPBOX_ACCESS_TOKEN,
            overlays = overlays,
            config = config,
            onMapClick = { geoPoint ->
                if (viewModel.isDrawingMode) {
                    viewModel.addPoint(geoPoint)
                    true
                } else {
                    false
                }
            },
            onOverlayClick = { id -> console.log("Overlay: $id") },
            modifier = Modifier.fillMaxSize(),
            extraHTML =
                buildOverlayButtons(
                    onProfileClick = onProfileClick,
                    onTrailsClick = onTrailsClick,
                    isDrawingMode = viewModel.isDrawingMode,
                    canUndo = viewModel.canUndo,
                    canComplete = viewModel.canComplete,
                    onStartRoute = { viewModel.startNewRoute() },
                    onUndo = { viewModel.undoLast() },
                    onCancel = { viewModel.cancelRoute() },
                    onComplete = { viewModel.completeRoute() },
                ),
        )
    }
}
