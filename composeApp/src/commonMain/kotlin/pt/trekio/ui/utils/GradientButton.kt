package pt.trekio.ui.utils

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.dummy_text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    gradientColors: List<Color> =
        listOf(
            Color(0xFF2196F3),
            Color(0xFF9C27B0),
        ),
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(15.dp)
    val renderedGradientColors =
        if (enabled) gradientColors else gradientColors.map { it.copy(alpha = 0.45f) }
    val gradient = Brush.horizontalGradient(colors = renderedGradientColors)

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        modifier = modifier,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContainerColor = Color.Transparent,
            ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(brush = gradient, shape = shape)
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(contentPadding),
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
