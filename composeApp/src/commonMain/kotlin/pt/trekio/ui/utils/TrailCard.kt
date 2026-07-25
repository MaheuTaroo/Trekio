package pt.trekio.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.trekio.misc.Metric
import pt.trekio.misc.TrailDifficulty
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.confirm_delete_button
import trekio.composeapp.generated.resources.difficulty_text
import trekio.composeapp.generated.resources.update_button
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailCard(
    name: String,
    distance: Double,
    difficulty: TrailDifficulty,
    metric: Metric,
    personal: Boolean,
    onUpdate: () -> Unit,
    onDelete: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable { showConfirm = true }
                .padding(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(35.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
        ) {
            Icon(
                imageVector = Icons.Default.Route,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "${round(distance * 100) / 100} ${metric.tag}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column {
            Text(
                text = stringResource(Res.string.difficulty_text),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = difficulty.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (personal) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                GradientButton(
                    onClick = onUpdate,
                    modifier = Modifier.width(90.dp).height(40.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.update_button),
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                GradientButton(
                    onClick = onDelete,
                    modifier = Modifier.width(90.dp).height(40.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.confirm_delete_button),
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrailCardPreview() = TrailCard("Trail 1", 10.0, TrailDifficulty.INTERMEDIATE, Metric.Kilometers, true, {}, {})
