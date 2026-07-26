package pt.trekio.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.trekio.misc.Metric
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.formatAsTime
import pt.trekio.misc.toComposableString
import pt.trekio.misc.toMiles
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.average_pacing_text
import trekio.composeapp.generated.resources.difficulty_text
import trekio.composeapp.generated.resources.stats_pace_format_km
import trekio.composeapp.generated.resources.stats_pace_format_mile
import trekio.composeapp.generated.resources.total_time_spent
import kotlin.math.round

@Composable
private fun averagePacingString(
    distance: Double,
    timeSpent: Long,
    metric: Metric,
): String {
    val paceSecondsPerMetric = if (distance > 0) (timeSpent / distance).toLong() else 0L
    return stringResource(
        if (metric == Metric.Kilometers) {
            Res.string.stats_pace_format_km
        } else {
            Res.string.stats_pace_format_mile
        },
        paceSecondsPerMetric / 60,
        paceSecondsPerMetric % 60,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HikeInfoCard(
    name: String,
    distance: Double,
    metric: Metric,
    difficulty: TrailDifficulty,
    timeSpent: Long,
) {
    val metricAccurateDistance = if (metric == Metric.Miles) distance.toMiles() else distance

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .padding(12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "${round(metricAccurateDistance * 100) / 100} ${metric.tag}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${stringResource(Res.string.total_time_spent)}: ${timeSpent.formatAsTime()}",
                style = MaterialTheme.typography.titleSmall,
            )

            Text(
                text =
                    stringResource(
                        Res.string.average_pacing_text,
                        averagePacingString(metricAccurateDistance, timeSpent, metric),
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${stringResource(Res.string.difficulty_text)}: ${difficulty.toComposableString()}",
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HikeInfoCardPreview() = HikeInfoCard("Trail 1", 10.0, Metric.Kilometers, TrailDifficulty.INTERMEDIATE, 10000)
