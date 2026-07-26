package pt.trekio.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import io.github.tiagopraia.kmp.mapbox.AnchoredOverlay
import io.github.tiagopraia.kmp.mapbox.GeographicPoint
import io.github.tiagopraia.kmp.mapbox.config.AndroidMapConfig
import io.github.tiagopraia.kmp.mapbox.configs.CircleOverlay
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapOverlays
import io.github.tiagopraia.kmp.mapbox.configs.MapStyle
import io.github.tiagopraia.kmp.mapbox.configs.PolylineOverlay
import io.github.tiagopraia.kmp.mapbox.map.AndroidMapWrapper
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import pt.trekio.BuildKonfig
import pt.trekio.R
import pt.trekio.misc.ColorPalette
import pt.trekio.misc.HaversineDistance
import pt.trekio.misc.Metric
import pt.trekio.misc.format
import pt.trekio.misc.formatAsTime
import pt.trekio.misc.toGeoPoint
import pt.trekio.misc.toMiles
import pt.trekio.ui.theme.ThemeMode
import pt.trekio.ui.theme.TrekioAppTheme
import pt.trekio.ui.utils.GradientButton
import pt.trekio.viewmodels.HikerSelection
import pt.trekio.viewmodels.HikingViewModel
import pt.trekio.viewmodels.SettingsViewModel
import pt.trekio.viewmodels.states.HikeState
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.arrived_text
import trekio.composeapp.generated.resources.distance_text
import trekio.composeapp.generated.resources.hiking_error
import trekio.composeapp.generated.resources.stats_pace_format_km
import trekio.composeapp.generated.resources.stats_pace_format_mile
import kotlin.time.Duration.Companion.seconds

private val OVERLAY_TINT = Color.Black
private val OVERLAY_SHAPE = RoundedCornerShape(8.dp)
private val OVERLAY_MOD = Modifier.size(48.dp)
private const val BLUE_MUTED = "#1B4F72"

private val HIKER_COLOR_PALETTE = ColorPalette.entries.filterNot { it.name.contains("BLUE", ignoreCase = true) }

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
    metric: Metric,
    state: HikeState,
    content: @Composable BoxScope.() -> Unit = { },
) {
    val isInDarkTheme = theme == ThemeMode.DARK || (theme == ThemeMode.SYSTEM && isSystemInDarkTheme())
    val config =
        remember {
            AndroidMapConfig(
                mapConfig = MapConfig(styleUri = if (isInDarkTheme) MapStyle.DARK else MapStyle.OUTDOORS),
            )
        }
    val lastCheckPointIndex = vm.checkpoint?.let { vm.path.indexOf(it) } ?: -1
    val overlays = combinedOverlays(vm, config, lastCheckPointIndex)
    val hikerSelection by vm.hikerSelection.collectAsState()
    val anchoredOverlay = hikerAnchoredOverlay(hikerSelection)

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidMapWrapper(
            overlays = overlays,
            anchoredOverlays = anchoredOverlay,
            accessToken = BuildKonfig.MAPBOX_ACCESS_TOKEN,
            config = config,
            onOverlayClick = { id, _ -> id.removePrefix("hiker_").toULongOrNull()?.let { vm.selectHiker(it) } },
            onMapClick = {
                vm.clearHikerSelection()
                true
            },
            onLocationUpdate = vm::reportLocation,
            modifier = Modifier.fillMaxSize(),
        )

        HikeActionButtons(vm, state, lastCheckPointIndex)
        DistanceToNextCard(vm, metric, lastCheckPointIndex)
        content()
        GpsDisabledScrim(vm, state)
    }
}

@Composable
private fun hikerAnchoredOverlay(selection: HikerSelection): List<AnchoredOverlay> {
    val (point, id) =
        when (selection) {
            is HikerSelection.Loading -> selection.location to selection.userId
            is HikerSelection.Loaded -> selection.location to selection.userId
            is HikerSelection.Error -> selection.location to selection.userId
            HikerSelection.None -> return emptyList()
        }

    val titleStyle = MaterialTheme.typography.titleMedium

    val nameText =
        when (selection) {
            is HikerSelection.Loaded -> selection.user.username
            is HikerSelection.Error -> "User #$id"
            is HikerSelection.Loading -> "..."
            HikerSelection.None -> ""
        }

    val cardPadding = 16.dp
    val nameWidth = measureTextWidth(nameText, titleStyle)
    val cardWidth = (nameWidth + cardPadding).coerceIn(80.dp, 180.dp)
    val cardHeight = measureTextHeight(nameText, titleStyle, cardWidth - cardPadding) + cardPadding

    return remember(selection, cardWidth, cardHeight) {
        listOf(
            AnchoredOverlay(
                id = "hiker_${id}_callout",
                point = point,
                widthDp = cardWidth.value.toDouble(),
                heightDp = cardHeight.value.toDouble(),
                content = {
                    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.width(cardWidth)) {
                        Text(
                            text = nameText,
                            style = titleStyle,
                            modifier = Modifier.padding(cardPadding / 2),
                        )
                    }
                },
            ),
        )
    }
}

private fun isTraveled(
    index: Int,
    lastCheckPointIndex: Int,
    isFirstPoint: Boolean,
    pathLastIndex: Int,
): Boolean {
    val startIndex = if (isFirstPoint) 0 else pathLastIndex
    if (index == startIndex) return true
    if (lastCheckPointIndex < 0) return false
    return if (isFirstPoint) index <= lastCheckPointIndex else index >= lastCheckPointIndex
}

private fun generateCircles(
    vm: HikingViewModel,
    config: AndroidMapConfig,
    lastCheckPointIndex: Int,
): List<CircleOverlay> =
    vm.path.mapIndexed { index, point ->
        val color =
            if (isTraveled(
                    index,
                    lastCheckPointIndex,
                    vm.isFirstPoint,
                    vm.path.lastIndex,
                )
            ) {
                BLUE_MUTED
            } else {
                ColorPalette.BLUE.hex
            }
        CircleOverlay("current_route_p$index", point, config.mapConfig.pointRadius, color, isClickable = false)
    }

private fun traveledRange(
    vm: HikingViewModel,
    lastCheckPointIndex: Int,
): Pair<Int, Int> = if (vm.isFirstPoint) 0 to lastCheckPointIndex else lastCheckPointIndex to vm.path.lastIndex

private fun remainingRange(
    vm: HikingViewModel,
    lastCheckPointIndex: Int,
): Pair<Int, Int> = if (vm.isFirstPoint) lastCheckPointIndex to vm.path.lastIndex else 0 to lastCheckPointIndex

private fun rangePolyline(
    id: String,
    vm: HikingViewModel,
    config: AndroidMapConfig,
    range: Pair<Int, Int>,
    colorHex: String,
): PolylineOverlay? {
    val (from, to) = range
    if (from >= to) return null
    return PolylineOverlay(
        id,
        vm.path.subList(from, to + 1),
        colorHex = colorHex,
        width = config.mapConfig.lineWidth,
        isClickable = false,
    )
}

private fun generatePolylines(
    vm: HikingViewModel,
    config: AndroidMapConfig,
    lastCheckPointIndex: Int,
): List<PolylineOverlay> {
    if (lastCheckPointIndex < 0) {
        return listOfNotNull(
            PolylineOverlay(
                "current_route_remaining",
                vm.path,
                colorHex = ColorPalette.BLUE.hex,
                width = config.mapConfig.lineWidth,
                isClickable = false,
            ),
        )
    }

    return listOfNotNull(
        rangePolyline("current_route_traveled", vm, config, traveledRange(vm, lastCheckPointIndex), BLUE_MUTED),
        rangePolyline("current_route_remaining", vm, config, remainingRange(vm, lastCheckPointIndex), ColorPalette.BLUE.hex),
    )
}

fun generateOverlays(
    vm: HikingViewModel,
    config: AndroidMapConfig,
    lastCheckPointIndex: Int,
): MapOverlays =
    MapOverlays(
        circles = generateCircles(vm, config, lastCheckPointIndex),
        polylines = generatePolylines(vm, config, lastCheckPointIndex),
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
        Logger.i(tag = "HikingScreen") { "STATE: $state" }
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

    HikingStateScreen(vm, theme = theme, metric = metric, state = state) {
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
                ConfirmDialog(
                    icon = Icons.Filled.Cancel,
                    title = stringResource(R.string.about_to_cancel_title),
                    text = stringResource(R.string.about_to_cancel_text),
                    confirmText = stringResource(R.string.yes_text),
                    onDismiss = vm::goBackToHike,
                    onAction = vm::cancel,
                    isDanger = false,
                    wantDismissButton = true,
                )

            HikeState.AboutToFinish ->
                ConfirmDialog(
                    icon = Icons.Filled.Flag,
                    title = stringResource(R.string.about_to_finish_title),
                    text = stringResource(R.string.confirmation_finish_text),
                    confirmText = stringResource(R.string.yes_text),
                    onDismiss = vm::goBackToHike,
                    onAction = vm::finish,
                    isDanger = false,
                    wantDismissButton = true,
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

private fun nextPointIndex(
    vm: HikingViewModel,
    lastCheckPointIndex: Int,
): Int {
    val direction = if (vm.isFirstPoint) 1 else -1
    val startIndex = if (vm.isFirstPoint) 0 else vm.path.lastIndex
    val currentIndex = if (lastCheckPointIndex >= 0) lastCheckPointIndex else startIndex
    return (currentIndex + direction).coerceIn(0, vm.path.lastIndex)
}

@Composable
fun Details(
    vm: HikingViewModel,
    state: HikeState,
    metric: Metric,
) {
    val (start, finish, distance) = (state as HikeState.Details)
    val duration = (finish - start) / 1000

    val distanceMetric = if (metric == Metric.Miles) distance.toMiles() else distance
    val distanceMetricText = stringResource(Res.string.distance_text, distance.format(3), metric.tag)

    val paceSecondsPerMetric = if (distanceMetric > 0) (duration / distanceMetric).toLong() else 0L
    val paceMinutes = paceSecondsPerMetric / 60
    val paceSeconds = (paceSecondsPerMetric % 60).toString().padStart(2, '0')

    val averageSpeedText =
        if (metric == Metric.Kilometers) {
            stringResource(Res.string.stats_pace_format_km, paceMinutes, paceSeconds)
        } else {
            stringResource(Res.string.stats_pace_format_mile, paceMinutes, paceSeconds)
        }

    LaunchedEffect(Unit) {
        delay(10.seconds)
        vm.details()
    }

    ConfirmDialog(
        icon = Icons.Filled.Celebration,
        title = stringResource(R.string.congratulations_text),
        text = "${duration.formatAsTime()}\n$averageSpeedText\n$distanceMetricText",
        confirmText = stringResource(R.string.close_text),
        onDismiss = vm::details,
        onAction = vm::details,
        isDanger = false,
        wantDismissButton = false,
    )
}

@Composable
fun rememberIsLocationEnabled(): MutableState<Boolean> {
    val context = LocalContext.current
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val state = remember { mutableStateOf(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) }

    DisposableEffect(Unit) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context,
                    intent: Intent,
                ) {
                    state.value = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                }
            }
        context.registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        onDispose { context.unregisterReceiver(receiver) }
    }

    return state
}

private fun colorForHiker(uid: ULong): String {
    val index = (uid % HIKER_COLOR_PALETTE.size.toULong()).toInt()
    return HIKER_COLOR_PALETTE[index].hex
}

private fun generateHikerCircles(
    hikers: Map<ULong, GeographicPoint>,
    config: AndroidMapConfig,
): List<CircleOverlay> =
    hikers.map { (uid, point) ->
        CircleOverlay(
            id = "hiker_$uid",
            center = point,
            radius = config.mapConfig.pointRadius,
            colorHex = colorForHiker(uid),
        )
    }

private fun combinedOverlays(
    vm: HikingViewModel,
    config: AndroidMapConfig,
    lastCheckPointIndex: Int,
): MapOverlays {
    val trailOverlays = generateOverlays(vm, config, lastCheckPointIndex)
    val hikerCircles = generateHikerCircles(vm.hikers, config)
    return trailOverlays.copy(circles = trailOverlays.circles + hikerCircles)
}

@Composable
private fun BoxScope.HikeActionButtons(
    vm: HikingViewModel,
    state: HikeState,
    lastCheckPointIndex: Int,
) {
    if (state != HikeState.Hiking) return

    val canFinish =
        (!vm.isFirstPoint && lastCheckPointIndex == 0) ||
            (vm.isFirstPoint && lastCheckPointIndex == vm.path.lastIndex)

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

        if (canFinish) {
            FloatingActionButton(
                onClick = vm::suggestFinishing,
                shape = OVERLAY_SHAPE,
                modifier = OVERLAY_MOD,
                content = ::FinishIcon,
            )
        }
    }
}

@Composable
private fun BoxScope.DistanceToNextCard(
    vm: HikingViewModel,
    metric: Metric,
    lastCheckPointIndex: Int,
) {
    val canFinish =
        (!vm.isFirstPoint && lastCheckPointIndex == 0) ||
            (vm.isFirstPoint && lastCheckPointIndex == vm.path.lastIndex)

    val text =
        if (canFinish) {
            stringResource(Res.string.arrived_text) // sem argumentos, sem placeholders
        } else {
            val nextIndex = nextPointIndex(vm, lastCheckPointIndex)
            val startIndex = if (vm.isFirstPoint) 0 else vm.path.lastIndex
            val referenceLocation = vm.lastReportedLocation ?: vm.path[startIndex]
            val distanceKm = HaversineDistance.between(referenceLocation.toGeoPoint(), vm.path[nextIndex].toGeoPoint())
            formatDistance(distanceKm, metric)
        }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 15.dp),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun GpsDisabledScrim(
    vm: HikingViewModel,
    state: HikeState,
) {
    val isGpsEnabled by rememberIsLocationEnabled()
    if (isGpsEnabled || state != HikeState.Hiking) return

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center,
    ) {
        FloatingActionButton(
            onClick = vm::suggestCancellation,
            shape = CircleShape,
        ) {
            Icon(Icons.Filled.Pause, contentDescription = null)
        }
    }
}

@Composable
fun formatDistance(
    distanceKm: Double,
    metric: Metric,
): String =
    if (metric == Metric.Kilometers) {
        if (distanceKm < 1.0) {
            "${(distanceKm * 1000).toInt()} m"
        } else {
            "${"%.3f".format(distanceKm)} ${metric.tag}"
        }
    } else {
        val distanceMiles = distanceKm.toMiles()
        if (distanceKm < 1.0) {
            "${(distanceMiles * 5280).toInt()} ft"
        } else {
            "${"%.3f".format(distanceMiles)} ${metric.tag}"
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmDialog(
    icon: ImageVector,
    title: String,
    text: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    isDanger: Boolean,
    wantDismissButton: Boolean,
    content: @Composable (() -> Unit)? = null,
) {
    val coloreScheme = MaterialTheme.colorScheme
    AlertDialog(
        icon = {
            DialogIcon(
                icon = icon,
                tint = if (isDanger) coloreScheme.error else coloreScheme.primary,
                container = if (isDanger) coloreScheme.error.copy(alpha = 0.25f) else coloreScheme.primary.copy(alpha = 0.25f),
            )
        },
        title = { Text(title) },
        text = {
            Text(text)
            content?.invoke()
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            GradientButton(onAction, modifier = Modifier.fillMaxWidth()) {
                Text(confirmText)
            }
        },
        dismissButton = {
            if (wantDismissButton) {
                TextButton(onAction, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cancel_text))
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun FinishConfirmDialogPreview() =
    ConfirmDialog(
        icon = Icons.Default.Flag,
        title = "Finish",
        text = "Finish",
        confirmText = "Finish",
        onDismiss = {},
        onAction = {},
        isDanger = false,
        wantDismissButton = true,
    )

@Preview(showBackground = true)
@Composable
fun FinishConfirmDialogWithoutDismissPreview() =
    ConfirmDialog(
        icon = Icons.Default.Flag,
        title = "Finish",
        text = "Finish",
        confirmText = "Finish",
        onDismiss = {},
        onAction = {},
        isDanger = false,
        wantDismissButton = false,
    )

@Preview(showBackground = true)
@Composable
fun FinishDialogIconPreview() =
    TrekioAppTheme(ThemeMode.DARK) {
        DialogIcon(
            icon = Icons.Default.Flag,
            tint = MaterialTheme.colorScheme.primary,
            container = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        )
    }

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
