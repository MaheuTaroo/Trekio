package pt.trekio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
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
import pt.trekio.misc.Metric
import pt.trekio.misc.toMiles
import pt.trekio.ui.theme.ThemeMode
import pt.trekio.viewmodels.HikingViewModel
import pt.trekio.viewmodels.SettingsViewModel
import pt.trekio.viewmodels.states.HikeState
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.distance_text
import trekio.composeapp.generated.resources.hiking_error
import trekio.composeapp.generated.resources.stats_pace_format_km
import trekio.composeapp.generated.resources.stats_pace_format_mile
import trekio.composeapp.generated.resources.stats_time_format_h_m_s
import trekio.composeapp.generated.resources.stats_time_format_m_s
import trekio.composeapp.generated.resources.stats_time_format_s

private val OVERLAY_TINT = Color.Black
private val OVERLAY_SHAPE = RoundedCornerShape(8.dp)
private val OVERLAY_MOD = Modifier.size(48.dp)
private const val BLUE_MUTED = "#6E8CA3"

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
private fun DetailsIcon() {
    Icon(
        imageVector = Icons.Filled.Celebration,
        contentDescription = "Details of finished hike",
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
        val lastCheckPointIndex = vm.checkpoint?.let { vm.path.indexOf(it) } ?: -1

        val overlays =
            generateOverlays(
                vm = vm,
                config = config,
                lastCheckPointIndex = lastCheckPointIndex,
            )

        AndroidMapWrapper(
            overlays = overlays,
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
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(15.dp),
            ) {
                FloatingActionButton(
                    onClick = vm::suggestCancellation,
                    shape = OVERLAY_SHAPE,
                    containerColor = Color.Red,
                    modifier = OVERLAY_MOD,
                    content = ::CancelIcon,
                )
                if (
                    (vm.isFirstPoint && lastCheckPointIndex == 0) ||
                    (!vm.isFirstPoint && lastCheckPointIndex == vm.path.lastIndex)
                ) {
                    FloatingActionButton(
                        onClick = vm::suggestFinishing,
                        shape = OVERLAY_SHAPE,
                        modifier = OVERLAY_MOD,
                        content = ::FinishIcon,
                    )
                }
            }
        }

        content()
    }
}

fun generateOverlays(
    vm: HikingViewModel,
    config: AndroidMapConfig,
    lastCheckPointIndex: Int,
): MapOverlays =
    MapOverlays(
        circles =
            vm.path.mapIndexed { index, point ->
                CircleOverlay(
                    "current_route_p$index",
                    point,
                    config.mapConfig.pointRadius,
                    if (lastCheckPointIndex >= 0 && index <= lastCheckPointIndex) {
                        BLUE_MUTED
                    } else {
                        ColorPalette.BLUE.hex
                    },
                )
            },
        polylines =
            buildList {
                if (lastCheckPointIndex > 0) {
                    add(
                        PolylineOverlay(
                            "current_route_traveled",
                            vm.path.subList(0, lastCheckPointIndex + 1),
                            colorHex = BLUE_MUTED,
                            width = config.mapConfig.lineWidth,
                            isClickable = false,
                        ),
                    )
                }
                if (lastCheckPointIndex < vm.path.lastIndex) {
                    val from = if (lastCheckPointIndex >= 0) lastCheckPointIndex else 0
                    add(
                        PolylineOverlay(
                            "current_route_remaining",
                            vm.path.subList(from, vm.path.size),
                            colorHex = ColorPalette.BLUE.hex,
                            width = config.mapConfig.lineWidth,
                            isClickable = false,
                        ),
                    )
                }
            },
    )

@Composable
actual fun HikingScreen(
    vm: HikingViewModel,
    settings: SettingsViewModel,
    onStop: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val theme by settings.theme.collectAsState()
    val metric by settings.metric.collectAsState()

    LaunchedEffect(state) {
        if (state == HikeState.Stopped) onStop()
    }

    if (state == HikeState.Loading) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            CircularProgressIndicator()
        }
    }

    HikingStateScreen(vm, theme = theme, state = state) {
        when (state) {
            is HikeState.Error ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    Arrangement.Center,
                    Alignment.CenterHorizontally,
                ) {
                    Text(String.format(stringResource(Res.string.hiking_error), (state as HikeState.Error).message))
                }

            HikeState.AboutToCancel ->
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

            HikeState.AboutToFinish ->
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

            HikeState.Stopping ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator()
                }

            is HikeState.Details -> Details(vm, state, metric)

            else -> Unit
        }
    }
}

@Composable
fun Details(
    vm: HikingViewModel,
    state: HikeState,
    metric: Metric,
) {
    val (start, finish, distance) = (state as HikeState.Details)
    val duration = finish - start

    val durationText =
        when {
            duration >= 3600 -> { // h m s
                val hours = duration / 3600
                val minutes = (duration / 60) % 60
                val seconds = duration % 60
                stringResource(Res.string.stats_time_format_h_m_s, hours, minutes, seconds)
            }
            duration > 60 -> { // m s
                val minutes = (duration / 60) % 60
                val seconds = duration % 60
                stringResource(Res.string.stats_time_format_m_s, minutes, seconds)
            }
            else -> stringResource(Res.string.stats_time_format_s, duration % 60) // s
        }

    val distanceMetric = if (metric == Metric.Miles) distance.toMiles() else distance
    val distanceMetricText = stringResource(Res.string.distance_text, "%.3f".format(distance), metric.tag)

    val paceSecondsPerMetric = if (distanceMetric > 0) (duration / distanceMetric).toLong() else 0L
    val paceMinutes = paceSecondsPerMetric / 60
    val paceSeconds = paceSecondsPerMetric % 60

    val averageSpeedText =
        if (metric == Metric.Kilometers) {
            stringResource(Res.string.stats_pace_format_km, paceMinutes, paceSeconds)
        } else {
            stringResource(Res.string.stats_pace_format_mile, paceMinutes, paceSeconds)
        }

    AlertDialog(
        icon = ::DetailsIcon,
        title = { Text("Congratulations!") },
        text = {
            Text(durationText)
            Text(averageSpeedText)
            Text(distanceMetricText)
        },
        onDismissRequest = vm::details,
        confirmButton = {
            TextButton(vm::details) {
                Text("Close")
            }
        },
        dismissButton = null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogIcon(
    icon: ImageVector,
    tint: Color,
    container: Color,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(45.dp)
                .clip(CircleShape)
                .background(container),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FinishDialogIconPreview() =
    DialogIcon(
        icon = Icons.Default.Flag,
        tint = MaterialTheme.colorScheme.primary,
        container = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
    )

@Preview(showBackground = true)
@Composable
fun CancelDialogIconPreview() =
    DialogIcon(
        icon = Icons.Default.Cancel,
        tint = MaterialTheme.colorScheme.error,
        container = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
    )

@Preview(showBackground = true)
@Composable
fun SuccessDialogIconPreview() =
    DialogIcon(
        icon = Icons.Filled.Celebration,
        tint = MaterialTheme.colorScheme.primary,
        container = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
    )
