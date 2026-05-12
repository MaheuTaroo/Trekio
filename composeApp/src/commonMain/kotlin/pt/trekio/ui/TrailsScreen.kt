package pt.trekio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
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
import pt.trekio.ui.utils.FilterButton
import pt.trekio.ui.utils.TopBarCreator
import pt.trekio.ui.utils.TrailCard
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.base_trails_text
import trekio.composeapp.generated.resources.personal_trails_text
import trekio.composeapp.generated.resources.search_trails_text
import trekio.composeapp.generated.resources.trails_title
import trekio.composeapp.generated.resources.verified_trails_text
import androidx.compose.foundation.lazy.items
import pt.trekio.ui.utils.GradientButton

data class TrailUi(
    val id: String,
    val name: String,
    val distanceKm: Double,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailsScreen(
    onBack: () -> Unit,
    onTrailCreation: () -> Unit,
    onStart: (TrailUi) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var personal by remember { mutableStateOf(false) }
    var verified by remember { mutableStateOf(false) }
    var base by remember { mutableStateOf(false) }

    val trails = listOf(
        /* TODO: replace with real data */
        TrailUi("1", "Pine Loop", 6.3),
        TrailUi("2", "River Walk", 4.8),
        TrailUi("3", "Summit Path", 10.2),
    )

    TopBarCreator(stringResource(Res.string.trails_title), onBack)

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 80.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text(stringResource(Res.string.search_trails_text)) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp)
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterButton(
                label = stringResource(Res.string.personal_trails_text),
                selected = personal,
                onToggle = { personal = !personal },
            )
            Spacer(Modifier.width(8.dp))
            FilterButton(
                label = stringResource(Res.string.verified_trails_text),
                selected = verified,
                onToggle = { verified = !verified },
            )
            Spacer(Modifier.width(8.dp))
            FilterButton(
                label = stringResource(Res.string.base_trails_text),
                selected = base,
                onToggle = { base = !base },
            )
        }

        Spacer(Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .padding(top = 90.dp, start = 20.dp, end = 20.dp),
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 6.dp),
            ) {
                items(items = trails, key = { it.id }) { trail ->
                    TrailCard(
                        name = trail.name,
                        distance = trail.distanceKm,
                        onClick = { onStart(trail) },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        GradientButton(
            onClick = onTrailCreation,
            modifier = Modifier.width(250.dp),
        ) {
            Text("Create Trail")
        }
    }
}

@Preview
@Composable
fun TrailsScreenPreview() =
    TrailsScreen({}, {}, {})