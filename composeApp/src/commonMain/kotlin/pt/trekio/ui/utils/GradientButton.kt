package pt.trekio.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.dummy_text
import kotlin.math.max

private data class RippleInstance(
    val offset: Offset,
    val radius: Animatable<Float, *> = Animatable(0f, Float.VectorConverter),
    val alpha: Animatable<Float, *> = Animatable(0.45f, Float.VectorConverter),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(15.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    gradientColors: List<Color>? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val resolvedColors =
        gradientColors ?: listOf(
            lerp(accent, Color.White, 0.18f),
            accent,
            lerp(accent, Color.Black, 0.18f),
        )
    val renderedGradientColors =
        if (enabled) resolvedColors else resolvedColors.map { it.copy(alpha = 0.45f) }
    val gradient = Brush.horizontalGradient(colors = renderedGradientColors)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh,
            ),
    )
    val ripples = remember { mutableStateListOf<RippleInstance>() }
    val scope = rememberCoroutineScope()
    val rippleColor = MaterialTheme.colorScheme.onPrimary

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        modifier =
            modifier.graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                transformOrigin = TransformOrigin.Center
            },
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = Color.Transparent,
            ),
        contentPadding = PaddingValues(0.dp),
        interactionSource = interactionSource,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(brush = gradient, shape = shape)
                    .defaultMinSize(minHeight = 48.dp)
                    .then(
                        if (enabled) {
                            Modifier.pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                    val maxDimension = max(size.width, size.height).toFloat()
                                    val targetRadius = maxDimension * 1.3f
                                    val instance = RippleInstance(offset = down.position)
                                    ripples.add(instance)
                                    scope.launch {
                                        instance.radius.animateTo(
                                            targetRadius,
                                            tween(durationMillis = 500),
                                        )
                                    }
                                    scope.launch {
                                        instance.alpha.animateTo(
                                            0f,
                                            tween(durationMillis = 500),
                                        )
                                        ripples.remove(instance)
                                    }
                                }
                            }
                        } else {
                            Modifier
                        },
                    ).drawBehind {
                        ripples.forEach { ripple ->
                            drawCircle(
                                color = rippleColor.copy(alpha = ripple.alpha.value),
                                radius = ripple.radius.value,
                                center = ripple.offset,
                            )
                        }
                    }.padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GradientButtonPreview() = GradientButton(onClick = {}) { Text(stringResource(Res.string.dummy_text)) }
