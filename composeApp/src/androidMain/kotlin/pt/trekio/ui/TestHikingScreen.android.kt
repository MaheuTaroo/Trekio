package pt.trekio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.tiagopraia.kmp.mapbox.config.AndroidMapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapStyle
import io.github.tiagopraia.kmp.mapbox.map.AndroidMapWrapper
import pt.trekio.BuildKonfig
import pt.trekio.viewmodels.TestHikingViewModel
import pt.trekio.viewmodels.states.TrailState

@Composable
actual fun TestHikingScreen(
    vm: TestHikingViewModel
) {
    val theme = isSystemInDarkTheme()
    val config =
        remember {
            AndroidMapConfig(
                mapConfig =
                    MapConfig(
                        styleUri = if (theme) MapStyle.DARK else MapStyle.OUTDOORS,
                    ),
            )
        }
    var mapReady by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidMapWrapper(
            accessToken = BuildKonfig.MAPBOX_ACCESS_TOKEN,
            config = config,
            onMapReady = { mapReady = true },
            modifier = Modifier.fillMaxSize(),
        )

        if (mapReady) {
            MapOverlayButtons(
                followButtonConfig = config.followButton,
                isDrawingMode = false,
                canUndo = false,
                canComplete = false,
                hasCompleted = false,
                routeName = "",
                trailState = TrailState.Idle,
                onRouteNameChange = { },
                onProfileClick = { },
                onTrailsClick = { },
                onStartRoute = { },
                onUndo = { },
                onCancel = { },
                onComplete = { },
                onCommit = { },
            )
        }
    }
}