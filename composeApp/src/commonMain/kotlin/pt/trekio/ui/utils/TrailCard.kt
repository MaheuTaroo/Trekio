package pt.trekio.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    onPreview: () -> Unit,
    onUpdate: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable { onPreview() }
                .padding(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(35.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
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

            Column(
                modifier = if (personal) Modifier.padding(end = 56.dp) else Modifier,
            ) {
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
        }

        if (personal) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                IconButton(
                    onClick = onUpdate,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(Res.string.update_button),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.confirm_delete_button),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrailCardPreview() = TrailCard("Trail 1", 10.0, TrailDifficulty.INTERMEDIATE, Metric.Kilometers, true, {}, {}, {})
