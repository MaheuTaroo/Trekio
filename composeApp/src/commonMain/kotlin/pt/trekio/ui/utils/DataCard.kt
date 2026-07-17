package pt.trekio.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.trekio.ui.theme.TrekioTheme
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.dummy_text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCard(
    label: StringResource,
    data: String,
) {
    DataCardContainer(label = label) {
        Text(
            text = data,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatStat(
    value: Float,
    decimals: Int,
    suffix: String,
): String {
    if (decimals <= 0) {
        return value.roundToIntSafe().toString() + suffix
    }
    var multiplier = 1
    repeat(decimals) { multiplier *= 10 }
    val rounded = (value * multiplier).roundToIntSafe()
    val whole = rounded / multiplier
    val fraction = (rounded % multiplier).let { if (it < 0) -it else it }
    val fractionStr = fraction.toString().padStart(decimals, '0')
    return "$whole.$fractionStr $suffix"
}

private fun Float.roundToIntSafe(): Int = kotlin.math.round(this).toInt()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCard(
    label: StringResource,
    value: Float,
    decimals: Int = 1,
    suffix: String = "",
) {
    var animationTarget by remember { mutableStateOf(0f) }
    val animatedValue by animateFloatAsState(
        targetValue = animationTarget,
        animationSpec = tween(durationMillis = 1100),
    )

    LaunchedEffect(value) {
        animationTarget = value
    }

    DataCardContainer(label = label) {
        Text(
            text = formatStat(animatedValue, decimals, suffix),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataCardContainer(
    label: StringResource,
    content: @Composable () -> Unit,
) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        )
    }

    Box(
        modifier =
            Modifier
                .width(250.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    transformOrigin = TransformOrigin.Center
                }.clip(RoundedCornerShape(10.dp))
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                ).padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.titleMedium,
                color = TrekioTheme.extendedColors.onSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DataCardPreview() = DataCard(Res.string.dummy_text, "Dummy")

@Preview(showBackground = true)
@Composable
fun DataCardNumericPreview() = DataCard(Res.string.dummy_text, value = 12.4f, decimals = 1, suffix = " km")
