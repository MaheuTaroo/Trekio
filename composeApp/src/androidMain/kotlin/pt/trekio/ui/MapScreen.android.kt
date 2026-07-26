package pt.trekio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.tiagopraia.kmp.mapbox.AnchoredOverlay
import io.github.tiagopraia.kmp.mapbox.GeographicPoint
import io.github.tiagopraia.kmp.mapbox.config.AndroidMapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapOverlays
import io.github.tiagopraia.kmp.mapbox.configs.MapStyle
import io.github.tiagopraia.kmp.mapbox.map.AndroidMapWrapper
import pt.trekio.BuildKonfig
import pt.trekio.R
import pt.trekio.dto.TrailDto
import pt.trekio.misc.HaversineDistance
import pt.trekio.misc.HaversineDistance.DISTANCE_OFF_TRAIL_THRESHOLD_FEET
import pt.trekio.misc.HaversineDistance.DISTANCE_OFF_TRAIL_THRESHOLD_METERS
import pt.trekio.misc.Metric
import pt.trekio.misc.showAlert
import pt.trekio.misc.toGeoPoint
import pt.trekio.repos.UserRepository
import pt.trekio.ui.theme.ThemeMode
import pt.trekio.viewmodels.MapViewModel
import pt.trekio.viewmodels.SettingsViewModel
import pt.trekio.viewmodels.states.SettingsState
import pt.trekio.viewmodels.states.TrailState

@Composable
actual fun MapScreen(
    viewModel: MapViewModel,
    onProfileClick: () -> Unit,
    onTrailsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onHikeClick: (TrailDto, Boolean) -> Unit,
    settingsVm: SettingsViewModel,
    userRepo: UserRepository,
) {
    val theme by settingsVm.theme.collectAsState()
    val settingsState by settingsVm.state.collectAsState()
    val trails by viewModel.trails.collectAsState()
    var rank by remember { mutableStateOf<String?>(null) }
    var userLocation by remember { mutableStateOf<GeographicPoint?>(null) }

    LaunchedEffect(Unit) {
        rank = userRepo.getOwnDetails()?.rank
        viewModel.startGettingTrails()
    }

    LaunchedEffect(trails) {
        viewModel.updateSavedTrails(trails)
    }

    LaunchedEffect(settingsState) {
        if (settingsState == SettingsState.LoggedOut) {
            onLogoutClick()
            settingsVm.resetState()
        }
    }

    val config =
        remember {
            AndroidMapConfig(
                mapConfig =
                    MapConfig(
                        styleUri = if (theme == ThemeMode.DARK) MapStyle.DARK else MapStyle.OUTDOORS,
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

    val currState by viewModel.state.collectAsState()

    val anchoredOverlays =
        anchoredOverlays(
            viewModel = viewModel,
            currState = currState,
            overlays = overlays,
            userLocation = userLocation,
            onHikeClick = onHikeClick,
            settingsVm = settingsVm,
        )

    MapBoxScreen(
        viewModel = viewModel,
        config = config,
        overlays = overlays,
        anchoredOverlays = anchoredOverlays,
        onProfileClick = onProfileClick,
        onTrailsClick = onTrailsClick,
        onSettingsClick = onSettingsClick,
        onLocationChange = { userLocation = it },
        settingsVm = settingsVm,
        rank = rank,
        currState = currState,
    )
}

private object Configs {
    val canvasHeight = 24.dp
    val cardVerticalPadding = 24.dp
    val cardHorizontalPadding = 24.dp
    val maxCardWidth = 200.dp
    val startButtonPadding = 6.dp
    val contentSpacing = 8.dp
}

@Composable
fun anchoredOverlays(
    viewModel: MapViewModel,
    currState: TrailState,
    overlays: MapOverlays,
    userLocation: GeographicPoint?,
    onHikeClick: (TrailDto, Boolean) -> Unit,
    settingsVm: SettingsViewModel,
): List<AnchoredOverlay> {
    val selection = viewModel.selection
    val selectedTrail = if (selection != null) (currState as? TrailState.Details)?.trail else null
    val metric by settingsVm.metric.collectAsState()

    val anchorPoint: GeographicPoint? =
        selection?.let { sel ->
            overlays.circles.firstOrNull { it.id == sel.overlayId }?.center ?: sel.clickPoint
        }

    val showStartButton =
        selection?.let { sel -> viewModel.isStartOrEndPoint(sel.overlayId, viewModel.savedRoutes) } ?: 0

    val trailNameText = selectedTrail?.name.orEmpty()
    val distanceText = stringResource(R.string.distance_text, "%.3f".format(selectedTrail?.distance ?: 0.0), metric.tag)
    val distanceFakeText = stringResource(R.string.distance_text, "%.3f".format(999.999), metric.tag)
    val difficultyText = stringResource(R.string.difficulty_trail_text, selectedTrail?.difficulty?.name.orEmpty())

    val distanceToUserMeters: Double? =
        remember(userLocation, anchorPoint) {
            if (userLocation != null && anchorPoint != null) {
                HaversineDistance.between(userLocation.toGeoPoint(), anchorPoint.toGeoPoint()) * 1000
            } else {
                null
            }
        }

    val titleStyle = MaterialTheme.typography.titleMedium
    val bodyStyle = MaterialTheme.typography.bodySmall

    val distanceToUserText =
        distanceToUserMeters?.let {
            stringResource(R.string.distance_point_text, formatDistance(it / 1000, metric))
        }

    val distance = if (metric == Metric.Kilometers) DISTANCE_OFF_TRAIL_THRESHOLD_METERS else DISTANCE_OFF_TRAIL_THRESHOLD_FEET
    val isWithinRange = distanceToUserMeters?.let { it <= DISTANCE_OFF_TRAIL_THRESHOLD_METERS } ?: false

    val nameSingleLineWidth = measureTextWidth(trailNameText, titleStyle)
    val distanceWidth = measureTextWidth(distanceFakeText, bodyStyle)
    val difficultyWidth = measureTextWidth(difficultyText, bodyStyle)
    val distanceToUserWidth = distanceToUserText?.let { measureTextWidth(it, bodyStyle) } ?: 0.dp

    val cardWidth =
        maxOf(nameSingleLineWidth, distanceWidth, difficultyWidth, distanceToUserWidth)
            .coerceAtMost(Configs.maxCardWidth) + Configs.cardHorizontalPadding

    val startIconSize = (cardWidth * 0.16f).coerceAtLeast(30.dp)
    val startButtonHeight = startIconSize + Configs.startButtonPadding * 4

    val textAreaWidth = cardWidth - Configs.cardHorizontalPadding

    val nameHeight = measureTextHeight(trailNameText, titleStyle, textAreaWidth)
    val distanceHeight = measureTextHeight(distanceText, bodyStyle, textAreaWidth)
    val difficultyHeight = measureTextHeight(difficultyText, bodyStyle, textAreaWidth)
    val distanceToUserHeight =
        distanceToUserText?.let { measureTextHeight(it, bodyStyle, textAreaWidth) } ?: 0.dp

    val contentHeight = nameHeight + distanceHeight + difficultyHeight + distanceToUserHeight
    val actionAreaHeight = if (showStartButton != 0) Configs.contentSpacing + startButtonHeight else 0.dp
    val totalHeight = Configs.canvasHeight + Configs.cardVerticalPadding + contentHeight + actionAreaHeight

    val anchoredOverlays =
        remember(selection, anchorPoint, selectedTrail, cardWidth, totalHeight, showStartButton, distanceToUserText) {
            if (selection == null || anchorPoint == null || selectedTrail == null) return@remember emptyList()
            listOf(
                AnchoredOverlay(
                    id = "${selection.overlayId}-trail-callout",
                    point = anchorPoint,
                    widthDp = cardWidth.value.toDouble(),
                    heightDp = totalHeight.value.toDouble(),
                    content = {
                        TrailCalloutOverlay(
                            trail = selectedTrail,
                            trailNameText = trailNameText,
                            distanceText = distanceText,
                            difficultyText = difficultyText,
                            distanceToUserText = distanceToUserText,
                            distance = distance,
                            isWithinRange = isWithinRange,
                            cardWidth = cardWidth,
                            styleName = titleStyle,
                            styleBody = bodyStyle,
                            anchorLineHeight = Configs.canvasHeight,
                            cardPadding = Configs.cardHorizontalPadding,
                            startButtonHeight = startButtonHeight,
                            startButtonPadding = Configs.startButtonPadding,
                            contentSpacing = Configs.contentSpacing,
                            showStartButton = showStartButton,
                            onHikeClick = onHikeClick,
                            metric = metric,
                            vm = viewModel,
                        )
                    },
                ),
            )
        }

    return anchoredOverlays
}

@Composable
fun MapBoxScreen(
    viewModel: MapViewModel,
    config: AndroidMapConfig,
    overlays: MapOverlays,
    anchoredOverlays: List<AnchoredOverlay>,
    onProfileClick: () -> Unit,
    onTrailsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLocationChange: (GeographicPoint) -> Unit,
    settingsVm: SettingsViewModel,
    rank: String?,
    currState: TrailState,
) {
    var mapReady by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidMapWrapper(
            accessToken = BuildKonfig.MAPBOX_ACCESS_TOKEN,
            overlays = overlays,
            config = config,
            anchoredOverlays = anchoredOverlays,
            onOverlayClick = viewModel::overlayClick,
            onMapReady = { mapReady = true },
            onMapClick = { geoPoint ->
                if (viewModel.isDrawingMode) {
                    viewModel.addPoint(geoPoint)
                    true
                } else {
                    viewModel.clearSelection()
                    false
                }
            },
            onLocationUpdate = onLocationChange,
            modifier = Modifier.fillMaxSize(),
        )

        if (mapReady) {
            val text = stringResource(R.string.alert_text)
            MapOverlayButtons(
                followButtonConfig = config.followButton,
                isDrawingMode = viewModel.isDrawingMode,
                canUndo = viewModel.canUndo,
                canComplete = viewModel.canComplete,
                hasCompleted = viewModel.hasCompleted,
                routeName = viewModel.draftRouteName,
                trailState = currState,
                onRouteNameChange = viewModel::updateDraftRouteName,
                onProfileClick = onProfileClick,
                onTrailsClick = onTrailsClick,
                onStartRoute = { if (!viewModel.startNewRoute(rank)) showAlert(text) },
                onUndo = viewModel::undoLast,
                onCancel = viewModel::cancelRoute,
                onComplete = viewModel::completeRoute,
                onCommit = viewModel::commitRoute,
                onSettings = onSettingsClick,
                onLogout = settingsVm::logoutUser,
            )
        }
    }
}

@Composable
fun TrailCalloutOverlay(
    trail: TrailDto,
    trailNameText: String,
    distanceText: String,
    difficultyText: String,
    distanceToUserText: String?,
    distance: Double,
    isWithinRange: Boolean,
    cardWidth: Dp,
    styleName: TextStyle,
    styleBody: TextStyle,
    anchorLineHeight: Dp,
    cardPadding: Dp,
    startButtonHeight: Dp,
    startButtonPadding: Dp,
    contentSpacing: Dp,
    showStartButton: Int,
    onHikeClick: (TrailDto, Boolean) -> Unit,
    metric: Metric,
    vm: MapViewModel,
) {
    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier.width(cardWidth),
    ) {
        Canvas(modifier = Modifier.size(width = 2.dp, height = anchorLineHeight)) {
            drawLine(
                color = Color.Black,
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = 4f,
            )
        }
        Card(
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier.width(cardWidth),
        ) {
            Column(modifier = Modifier.padding(cardPadding / 2)) {
                Text(text = trailNameText, style = styleName)
                Text(text = distanceText, style = styleBody)
                Text(text = difficultyText, style = styleBody)
                distanceToUserText?.let { Text(text = it, style = styleBody) }

                if (showStartButton != 0) {
                    Spacer(modifier = Modifier.height(contentSpacing))
                    val outOfRangeText =
                        stringResource(
                            R.string.out_range_text,
                            distance,
                            if (metric ==
                                Metric.Kilometers
                            ) {
                                "meters"
                            } else {
                                "feet"
                            },
                        )
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        Button(
                            onClick = {
                                if (isWithinRange) {
                                    vm.clearSelection()
                                    onHikeClick(trail, showStartButton == 1)
                                } else {
                                    showAlert(outOfRangeText)
                                }
                            },
                            contentPadding = PaddingValues(startButtonPadding),
                            modifier =
                                Modifier
                                    .requiredWidth(cardWidth - cardPadding)
                                    .requiredHeight(startButtonHeight)
                                    .padding(vertical = startButtonPadding),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun measureTextWidth(
    text: String,
    style: TextStyle,
): Dp {
    val textMeasurer = rememberTextMeasurer()
    val widthInPixels = textMeasurer.measure(text, style).size.width
    return with(LocalDensity.current) { widthInPixels.toDp() } + 4.dp // margem de segurança
}

@Composable
fun measureTextHeight(
    text: String,
    style: TextStyle,
    maxWidth: Dp,
): Dp {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val maxWidthPx = with(density) { maxWidth.roundToPx() }
    val result =
        textMeasurer.measure(
            text = text,
            style = style,
            constraints =
                androidx.compose.ui.unit
                    .Constraints(maxWidth = maxWidthPx),
        )
    return with(density) { result.size.height.toDp() }
}
