package pt.trekio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.trekio.misc.Metric
import pt.trekio.misc.toMiles
import pt.trekio.services.FailingService
import pt.trekio.ui.utils.DataCard
import pt.trekio.ui.utils.TopBarCreator
import pt.trekio.ui.utils.titleIntermediate
import pt.trekio.viewmodels.SettingsViewModel
import pt.trekio.viewmodels.UserProfileViewModel
import pt.trekio.viewmodels.states.UserProfileState
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.statistics_text
import trekio.composeapp.generated.resources.total_km_text
import trekio.composeapp.generated.resources.total_mi_text
import trekio.composeapp.generated.resources.total_time_spent
import trekio.composeapp.generated.resources.total_trails_text
import trekio.composeapp.generated.resources.user_profile_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onBack: () -> Unit,
    vm: UserProfileViewModel,
    settingsVm: SettingsViewModel,
) {
    val currState by vm.state.collectAsState()
    val metric by settingsVm.metric.collectAsState()

    val isKm = metric == Metric.Kilometers

    LaunchedEffect(Unit) {
        vm.profileDetails()
    }

    val user = (currState as? UserProfileState.Success)?.user

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        TopBarCreator(stringResource(Res.string.user_profile_title), onBack)

        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(top = 130.dp),
        ) {
            UserInfo(
                username = user?.username ?: "",
                rank = user?.rank ?: "",
                totalTrails = 7f,
                totalDistance = if (isKm) 12.4 else (12.4).toMiles(),
                totalTime = 184f,
                isKm = isKm,
            )

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun UserProfileScreenPreview() =
    UserProfileScreen(
        {},
        UserProfileViewModel(FailingService),
        SettingsViewModel(
            FailingService,
            FailingService,
        ),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserInfo(
    username: String,
    rank: String,
    totalTrails: Float,
    totalDistance: Double,
    totalTime: Float,
    isKm: Boolean,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(90.dp),
            shape = CircleShape,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            color = MaterialTheme.colorScheme.primary,
        ) {
            Box(
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (username.isNotEmpty()) username.first().uppercase() else "",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Spacer(Modifier.height(15.dp))

        Text(
            text = username,
            style = titleIntermediate,
        )

        Spacer(Modifier.height(15.dp))

        Surface(
            shape = RoundedCornerShape(50.dp),
            color = MaterialTheme.colorScheme.primary,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector =
                        when (rank) {
                            "NEW" -> Icons.Default.Star
                            "VERIFIED" -> Icons.Default.Check
                            else -> Icons.Default.Star
                        },
                    contentDescription = null,
                )
                Text(
                    text = rank,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Spacer(Modifier.height(25.dp))

        Text(
            text = stringResource(Res.string.statistics_text),
            style = titleIntermediate,
        )

        Spacer(Modifier.height(15.dp))

        DataCard(Res.string.total_trails_text, value = totalTrails, decimals = 0)

        Spacer(Modifier.height(15.dp))

        DataCard(
            if (isKm) Res.string.total_km_text else Res.string.total_mi_text,
            value = totalDistance.toFloat(),
            decimals = 1,
            suffix = if (isKm) Metric.Kilometers.tag else Metric.Miles.tag,
        )

        Spacer(Modifier.height(15.dp))

        DataCard(Res.string.total_time_spent, value = totalTime, decimals = 0, suffix = "min")
    }
}
