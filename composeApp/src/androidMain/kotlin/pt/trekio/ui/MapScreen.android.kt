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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.tiagopraia.kmp.mapbox.AnchoredOverlay
import io.github.tiagopraia.kmp.mapbox.GeographicPoint
import io.github.tiagopraia.kmp.mapbox.config.AndroidMapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapStyle
import io.github.tiagopraia.kmp.mapbox.map.AndroidMapWrapper
import pt.trekio.BuildKonfig
import pt.trekio.dto.TrailDto
import pt.trekio.ui.theme.ThemeMode
import pt.trekio.viewmodels.MapViewModel
import pt.trekio.viewmodels.SettingsViewModel
import pt.trekio.viewmodels.states.TrailState

@Composable
actual fun MapScreen(
    viewModel: MapViewModel,
    onProfileClick: () -> Unit,
    onTrailsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onHikeClick: (TrailDto) -> Unit,
    settingsVm: SettingsViewModel,
) {
    val theme by settingsVm.theme.collectAsState()
    val config =
        remember {
            AndroidMapConfig(
                mapConfig =
                    MapConfig(
                        styleUri = if (theme == ThemeMode.DARK) MapStyle.DARK else MapStyle.OUTDOORS,
                    ),
            )
        }
    var mapReady by remember { mutableStateOf(false) }

    val overlays by remember(
        viewModel.savedRoutes,
        viewModel.draftPoints,
        viewModel.draftRouteId,
    ) {
        derivedStateOf { viewModel.buildOverlays(config.mapConfig) }
    }

    val selection = viewModel.selection
    val currState by viewModel.state.collectAsState()
    val selectedTrail = if (selection != null) (currState as? TrailState.Details)?.trail else null

    val anchorPoint: GeographicPoint? =
        selection?.let { sel ->
            overlays.circles.firstOrNull { it.id == sel.overlayId }?.center ?: sel.clickPoint
        }

    val canvasHeight = 24.dp
    val cardVerticalPadding = 24.dp
    val cardHorizontalPadding = 24.dp
    val maxCardWidth = 200.dp

    val titleStyle = MaterialTheme.typography.titleMedium
    val bodyStyle = MaterialTheme.typography.bodySmall

    val trailNameText = selectedTrail?.name.orEmpty()
    val distanceText = "${"%.3f".format(selectedTrail?.distance ?: 0.0)} km"
    val difficultyText = selectedTrail?.difficulty?.name.orEmpty()

    val nameSingleLineWidth = measureTextWidth(trailNameText, titleStyle)
    val distanceWidth = measureTextWidth(distanceText, bodyStyle)
    val difficultyWidth = measureTextWidth(difficultyText, bodyStyle)

    val cardWidth =
        maxOf(nameSingleLineWidth, distanceWidth, difficultyWidth).coerceAtMost(maxCardWidth) + cardHorizontalPadding

    val startIconSize = (cardWidth * 0.16f).coerceAtLeast(30.dp)
    val startButtonPadding = 6.dp
    val startButtonHeight = startIconSize + startButtonPadding * 4
    val contentSpacing = 8.dp

    val textAreaWidth = cardWidth - cardHorizontalPadding

    val nameHeight = measureTextHeight(trailNameText, titleStyle, textAreaWidth)
    val distanceHeight = measureTextHeight(distanceText, bodyStyle, textAreaWidth)
    val difficultyHeight = measureTextHeight(difficultyText, bodyStyle, textAreaWidth)

    val contentHeight = nameHeight + distanceHeight + difficultyHeight
    val totalHeight = canvasHeight + cardVerticalPadding + contentHeight + contentSpacing + startButtonHeight

    val anchoredOverlays =
        remember(selection, anchorPoint, selectedTrail, cardWidth, totalHeight) {
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
                            cardWidth = cardWidth,
                            styleName = titleStyle,
                            styleBody = bodyStyle,
                            anchorLineHeight = canvasHeight,
                            cardPadding = cardHorizontalPadding,
                            startButtonHeight = startButtonHeight,
                            startButtonPadding = startButtonPadding,
                            contentSpacing = contentSpacing,
                            onHikeClick = onHikeClick,
                        )
                    },
                ),
            )
        }

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
            modifier = Modifier.fillMaxSize(),
        )

        if (mapReady) {
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
                onStartRoute = viewModel::startNewRoute,
                onUndo = viewModel::undoLast,
                onCancel = viewModel::cancelRoute,
                onComplete = viewModel::completeRoute,
                onCommit = viewModel::commitRoute,
                onSettings = onSettingsClick,
                onLogout = {
                    settingsVm.logoutUser()
                    onLogoutClick()
                },
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
    cardWidth: Dp,
    styleName: TextStyle,
    styleBody: TextStyle,
    anchorLineHeight: Dp,
    cardPadding: Dp,
    startButtonHeight: Dp,
    startButtonPadding: Dp,
    contentSpacing: Dp,
    onHikeClick: (TrailDto) -> Unit,
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

                Spacer(modifier = Modifier.height(contentSpacing))

                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Button(
                        onClick = { onHikeClick(trail) },
                        contentPadding = PaddingValues(startButtonPadding),
                        modifier =
                            Modifier
                                .requiredWidth(cardWidth - cardPadding)
                                .requiredHeight(startButtonHeight)
                                .padding(vertical = startButtonPadding),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                            contentDescription = "Start Hike",
                            modifier = Modifier.fillMaxSize(),
                        )
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
    val widthInPixels = textMeasurer.measure(text, style).size.width + 1
    return with(LocalDensity.current) { widthInPixels.toDp() }
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
