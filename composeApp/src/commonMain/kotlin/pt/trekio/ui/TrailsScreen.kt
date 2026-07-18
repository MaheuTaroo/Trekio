package pt.trekio.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.trekio.dto.GeoPointDto
import pt.trekio.dto.TrailDto
import pt.trekio.misc.Metric
import pt.trekio.misc.TrailDifficulty
import pt.trekio.services.FailingService
import pt.trekio.ui.utils.FilterButton
import pt.trekio.ui.utils.TopBarCreator
import pt.trekio.ui.utils.TrailCard
import pt.trekio.viewmodels.SettingsViewModel
import pt.trekio.viewmodels.TrailFetchViewModel
import pt.trekio.viewmodels.states.TrailFetchState
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.base_trails_text
import trekio.composeapp.generated.resources.personal_trails_text
import trekio.composeapp.generated.resources.search_trails_text
import trekio.composeapp.generated.resources.trails_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailsScreen(
    vm: TrailFetchViewModel,
    onBack: () -> Unit,
    onStart: (TrailDto) -> Unit,
    settingsVm: SettingsViewModel,
) {
    var search by remember { mutableStateOf("") }
    var personal by remember { mutableStateOf(false) }
    var base by remember { mutableStateOf(false) }

    val state by vm.state.collectAsState()
    val metric by settingsVm.metric.collectAsState()

    LaunchedEffect(Unit) {
        if (state == TrailFetchState.Idle) vm.fetchPage()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        TopBarCreator(stringResource(Res.string.trails_title), onBack)

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 100.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .border(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(20.dp),
                        ).padding(horizontal = 15.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )

                Spacer(Modifier.width(5.dp))

                BasicTextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { innerTextField ->
                        if (search.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.search_trails_text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilterButton(
                    label = stringResource(Res.string.personal_trails_text),
                    selected = personal,
                    onToggle = { personal = !personal },
                    modifier = Modifier.weight(1f),
                )

                FilterButton(
                    label = stringResource(Res.string.base_trails_text),
                    selected = base,
                    onToggle = { base = !base },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(15.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                when (state) {
                    TrailFetchState.Idle -> {}

                    TrailFetchState.Loading -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is TrailFetchState.Success ->
                        TrailsColumn((state as TrailFetchState.Success).trails, onStart, metric)

                    is TrailFetchState.Error -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                text = (state as TrailFetchState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun TrailsScreenPreview() =
    TrailsScreen(
        TrailFetchViewModel(FailingService),
        {},
        {},
        SettingsViewModel(
            FailingService,
            FailingService,
        ),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.TrailsColumn(
    list: List<TrailDto>,
    onStart: (TrailDto) -> Unit,
    metric: Metric,
) {
    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 6.dp),
    ) {
        items(items = list, key = { it.id.toLong() }) { trail ->
            TrailCard(
                name = trail.name,
                distance = trail.distance,
                onClick = { onStart(trail) },
                metric = metric,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrailsColumnPreview() =
    Column(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        TrailsColumn(
            list =
                listOf(
                    TrailDto(
                        id = 1UL,
                        name = "Trail 1",
                        start = GeoPointDto(0.0, 0.0, 0.0),
                        end = GeoPointDto(0.0, 0.0, 0.0),
                        path = listOf(GeoPointDto(0.0, 0.0, 0.0)),
                        distance = 4.2,
                        difficulty = TrailDifficulty.BEGINNER,
                        parent = null,
                    ),
                    TrailDto(
                        id = 2UL,
                        name = "Trail 2",
                        start = GeoPointDto(0.0, 0.0, 0.0),
                        end = GeoPointDto(0.0, 0.0, 0.0),
                        path = listOf(GeoPointDto(0.0, 0.0, 0.0)),
                        distance = 10.0,
                        difficulty = TrailDifficulty.INTERMEDIATE,
                        parent = null,
                    ),
                ),
            onStart = {},
            metric = Metric.Kilometers,
        )
    }
