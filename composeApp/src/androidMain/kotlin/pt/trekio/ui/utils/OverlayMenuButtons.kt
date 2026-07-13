package pt.trekio.ui.utils

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.tiagopraia.kmp.mapbox.config.AndroidFollowButtonConfig
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

private val BUTTON_SIZE = 52.dp
private val OPTION_SIZE = 44.dp
private val OPTION_RADIUS = 100.dp
private val BUTTON_PAD_END = 16.dp
private val BUTTON_PAD_TOP = 24.dp

data class Option(
    val icon: ImageVector,
    val label: String,
    val angle: Float,
    val tint: Color = Color.Unspecified,
    val background: Color = Color.Unspecified,
    val onClick: () -> Unit,
)

private class OptionState(
    center: Offset,
) {
    val offset = Animatable(center, Offset.VectorConverter)
    val scale = Animatable(0f)
}

private suspend fun OptionState.animateOpen(target: Offset) =
    coroutineScope {
        val spec = spring<Offset>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
        launch { offset.animateTo(target, spec) }
        launch { scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
    }

private suspend fun OptionState.animateClose(target: Offset) =
    coroutineScope {
        val tween = tween<Offset>(200, easing = FastOutSlowInEasing)
        launch { offset.animateTo(target, tween) }
        launch { scale.animateTo(0f, tween(180, easing = FastOutSlowInEasing)) }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayMenuButtons(
    config: AndroidFollowButtonConfig,
    options: List<Option>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val density = LocalDensity.current
        val halfPx = with(density) { BUTTON_SIZE.toPx() / 2f }

        val restOffset =
            with(density) {
                Offset(maxWidth.toPx() - BUTTON_PAD_END.toPx() - halfPx, BUTTON_PAD_TOP.toPx() + halfPx)
            }
        val centerOffset =
            with(density) {
                Offset(maxWidth.toPx() / 2f, maxHeight.toPx() / 2f)
            }
        val radiusPx = with(density) { OPTION_RADIUS.toPx() }

        var isOpen by remember { mutableStateOf(false) }
        val buttonOffset = remember { Animatable(restOffset, Offset.VectorConverter) }
        val optionStates = remember { options.map { OptionState(centerOffset) } }

        val iconRotation by animateFloatAsState(
            targetValue = if (isOpen) 135f else 0f,
            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        )
        val scrimAlpha by animateFloatAsState(
            targetValue = if (isOpen) 0.75f else 0f,
            animationSpec = tween(400, easing = FastOutSlowInEasing),
        )

        LaunchedEffect(isOpen) {
            if (isOpen) {
                launch {
                    buttonOffset.animateTo(centerOffset, tween(450, easing = FastOutSlowInEasing))
                }
                delay(300.milliseconds)
                options.forEachIndexed { index, option ->
                    val rad = Math.toRadians(option.angle.toDouble())
                    val target = centerOffset + Offset(cos(rad).toFloat() * radiusPx, sin(rad).toFloat() * radiusPx)

                    launch {
                        delay((index * 40L).milliseconds)
                        optionStates[index].animateOpen(target)
                    }
                }
            } else {
                optionStates.forEach {
                    launch {
                        it.animateClose(centerOffset)
                    }
                }
                delay(150.milliseconds)
                buttonOffset.animateTo(restOffset, tween(450, easing = FastOutSlowInEasing))
            }
        }

        if (scrimAlpha > 0f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = scrimAlpha))
                        .clickable(remember { MutableInteractionSource() }, null, onClick = { isOpen = false }),
            )
        }

        options.forEachIndexed { index, option ->
            OverlayOptionButton(
                config = config,
                option = option,
                state = optionStates[index],
                sizePx = with(density) { BUTTON_SIZE.toPx() },
                onDismiss = { isOpen = false },
            )
        }

        OverlayMainButton(
            config = config,
            modifier = Modifier,
            offset = buttonOffset.value,
            halfSizePx = halfPx,
            isOpen = isOpen,
            iconRotation = iconRotation,
            onClick = { isOpen = !isOpen },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlayOptionButton(
    config: AndroidFollowButtonConfig,
    option: Option,
    state: OptionState,
    sizePx: Float,
    onDismiss: () -> Unit,
) {
    val position = state.offset.value
    val scale = state.scale.value

    var widthPx by remember { mutableIntStateOf(0) }

    Box(
        modifier =
            Modifier
                .offset { IntOffset((position.x - widthPx / 2f).roundToInt(), (position.y - sizePx / 2f).roundToInt()) }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = scale
                    transformOrigin = TransformOrigin.Center
                }.clickable(remember { MutableInteractionSource() }, null) {
                    option.onClick()
                },
    ) {
        Surface(
            modifier =
                Modifier.width(140.dp).onGloballyPositioned {
                    widthPx = it.size.width
                },
            shape = config.buttonShape,
            color = config.followButtonColor,
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.label,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Black,
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    option.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlayMainButton(
    config: AndroidFollowButtonConfig,
    modifier: Modifier,
    offset: Offset,
    halfSizePx: Float,
    isOpen: Boolean,
    iconRotation: Float,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            modifier
                .size(BUTTON_SIZE)
                .offset {
                    IntOffset((offset.x - halfSizePx).roundToInt(), (offset.y - halfSizePx).roundToInt())
                }.clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        shape = config.buttonShape,
        color = config.followButtonColor,
        shadowElevation = if (isOpen) 12.dp else 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isOpen) Icons.Filled.Close else Icons.Filled.Person,
                contentDescription = null,
                tint = Color.Black,
                modifier =
                    Modifier.size(24.dp).graphicsLayer {
                        rotationZ = iconRotation
                    },
            )
        }
    }
}
