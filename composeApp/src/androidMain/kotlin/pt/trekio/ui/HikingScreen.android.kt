package pt.trekio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

private val OVERLAY_TINT = Color.Black
private val OVERLAY_SHAPE = RoundedCornerShape(8.dp)
private val OVERLAY_MOD = Modifier.size(48.dp)

@Composable
private fun CancelIcon() {
    Icon(
        imageVector = Icons.Filled.Cancel,
        contentDescription = "Cancel hike",
        tint = OVERLAY_TINT,
    )
}

@Composable
private fun FinishIcon() {
    Icon(
        imageVector = Icons.Filled.Flag,
        contentDescription = "Finish hike",
        tint = OVERLAY_TINT,
    )
}

@Composable
fun HikingStateScreen(
    vm: HikingViewModel,
    theme: ThemeMode,
    state: HikeState,
    content: @Composable BoxScope.() -> Unit = { },
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
        if (state == HikeState.Hiking) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FloatingActionButton(
                    onClick = vm::suggestCancellation,
                    shape = OVERLAY_SHAPE,
                    containerColor = Color.Red,
                    modifier = OVERLAY_MOD,
                    content = ::CancelIcon,
                )
                FloatingActionButton(
                    onClick = vm::suggestFinishing,
                    shape = OVERLAY_SHAPE,
                    modifier = OVERLAY_MOD,
                    content = ::FinishIcon,
                )
            }
        }

        content()
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

        HikeState.Hiking -> HikingStateScreen(vm, theme, state)

        HikeState.AboutToCancel ->
            HikingStateScreen(vm, theme, state) {
                AlertDialog(
                    icon = ::CancelIcon,
                    title = { Text("Leaving so soon?") },
                    text = { Text("Are you sure you want to cancel your hike?") },
                    onDismissRequest = vm::goBackToHike,
                    confirmButton = {
                        TextButton(vm::cancel) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(vm::goBackToHike) {
                            Text("No")
                        }
                    },
                )
            }

        HikeState.AboutToFinish ->
            HikingStateScreen(vm, theme, state) {
                HikingStateScreen(vm, theme, state) {
                    AlertDialog(
                        icon = ::FinishIcon,
                        title = { Text("Leaving anyone behind?") },
                        text = { Text("Are you sure you want to finish your hike?") },
                        onDismissRequest = vm::goBackToHike,
                        confirmButton = {
                            TextButton(vm::finish) {
                                Text("Yes")
                            }
                        },
                        dismissButton = {
                            TextButton(vm::goBackToHike) {
                                Text("No")
                            }
                        },
                    )
                }
            }

        HikeState.Stopping ->
            HikingStateScreen(vm, theme, state) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator()
                }
            }

        HikeState.Stopped -> onStop()
    }
}
