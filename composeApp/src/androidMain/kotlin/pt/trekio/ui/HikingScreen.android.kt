package pt.trekio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.tiagopraia.kmp.mapbox.config.AndroidMapConfig
import io.github.tiagopraia.kmp.mapbox.configs.CircleOverlay
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapOverlays
import io.github.tiagopraia.kmp.mapbox.configs.MapStyle
import io.github.tiagopraia.kmp.mapbox.configs.PolylineOverlay
import io.github.tiagopraia.kmp.mapbox.map.AndroidMapWrapper
import org.jetbrains.compose.resources.stringResource
import pt.trekio.BuildKonfig
import pt.trekio.misc.ColorPalette
import pt.trekio.ui.theme.ThemeMode
import pt.trekio.viewmodels.HikingViewModel
import pt.trekio.viewmodels.SettingsViewModel
import pt.trekio.viewmodels.states.HikeState
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.hiking_error

@Composable
fun HikingStateScreen(
    vm: HikingViewModel,
    theme: ThemeMode,
) {
    val isInDarkTheme = theme == ThemeMode.DARK || (theme == ThemeMode.SYSTEM && isSystemInDarkTheme())
    val config =
        remember {
            AndroidMapConfig(
                mapConfig =
                    MapConfig(
                        styleUri = if (isInDarkTheme) MapStyle.DARK else MapStyle.OUTDOORS,
                    ),
            )
        }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidMapWrapper(
            overlays =
                MapOverlays(
                    circles =
                        vm.path.mapIndexed { index, point ->
                            CircleOverlay(
                                "current_route_p$index",
                                point,
                                config.mapConfig.pointRadius,
                                ColorPalette.BLUE.hex,
                            )
                        },
                    polylines =
                        listOf(
                            PolylineOverlay(
                                "current_route",
                                vm.path,
                                colorHex = ColorPalette.BLUE.hex,
                                width = config.mapConfig.lineWidth,
                                isClickable = false,
                            ),
                        ),
                ),
            accessToken = BuildKonfig.MAPBOX_ACCESS_TOKEN,
            config = config,
            onMapReady = { },
            onLocationUpdate = vm::reportLocation,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
actual fun HikingScreen(
    vm: HikingViewModel,
    settings: SettingsViewModel,
    onStop: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val theme by settings.theme.collectAsState()
    when (state) {
        HikeState.Loading ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                CircularProgressIndicator()
            }

        is HikeState.Error ->
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                Arrangement.Center,
                Alignment.CenterHorizontally,
            ) {
                Text(String.format(stringResource(Res.string.hiking_error), (state as HikeState.Error).message))
            }

        HikeState.Hiking -> HikingStateScreen(vm, theme)

        HikeState.Stopped -> onStop()
    }
}
