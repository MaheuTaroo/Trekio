package pt.trekio.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.cancel_text
import trekio.composeapp.generated.resources.start_text
import trekio.composeapp.generated.resources.start_trail_extended_text
import trekio.composeapp.generated.resources.start_trail_text
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailCard(
    name: String,
    distance: Double,
    onClick: () -> Unit,
    metric: Metric,
) {
    var showConfirm by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
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

        Spacer(Modifier.width(10.dp))

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

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = stringResource(Res.string.start_trail_text),
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.start_trail_extended_text),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        onClick()
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.start_text),
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirm = false },
                ) {
                    Text(
                        text = stringResource(Res.string.cancel_text),
                    )
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TrailCardPreview() = TrailCard("Trail 1", 10.0, {}, Metric.Kilometers)
