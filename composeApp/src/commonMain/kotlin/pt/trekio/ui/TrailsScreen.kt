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
import androidx.compose.material.icons.filled.Route
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.tiagopraia.kmp.mapbox.GeographicPoint
import org.jetbrains.compose.resources.stringResource
import pt.trekio.dto.GeoPointDto
import pt.trekio.dto.TrailDto
import pt.trekio.misc.Metric
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.toMiles
import pt.trekio.repos.UserRepository
import pt.trekio.services.FailingService
import pt.trekio.ui.utils.Action
import pt.trekio.ui.utils.ContentWarning
import pt.trekio.ui.utils.ContentWarningButtons
import pt.trekio.ui.utils.ContentWarningDialog
import pt.trekio.ui.utils.CustomTextField
import pt.trekio.ui.utils.FilterButton
import pt.trekio.ui.utils.TopBarCreator
import pt.trekio.ui.utils.TrailCard
import pt.trekio.viewmodels.SettingsViewModel
import pt.trekio.viewmodels.TrailFetchViewModel
import pt.trekio.viewmodels.states.TrailFetchState
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.new_trail_text
import trekio.composeapp.generated.resources.personal_trails_text
import trekio.composeapp.generated.resources.search_trails_text
import trekio.composeapp.generated.resources.trail_holder_text
import trekio.composeapp.generated.resources.trails_title
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailsScreen(
    vm: TrailFetchViewModel,
    onBack: () -> Unit,
    settingsVm: SettingsViewModel,
    userRepo: UserRepository,
) {
    var search by remember { mutableStateOf("") }
    var personal by remember { mutableStateOf(false) }

    val state by vm.state.collectAsState()
    val metric by settingsVm.metric.collectAsState()

    LaunchedEffect(state) {
        if (state == TrailFetchState.Idle) vm.fetchPage()
        if (state == TrailFetchState.Success && personal) vm.fetchPersonalTrails()
    }

    LaunchedEffect(search) {
        if (search.isNotEmpty()) vm.fetchTrailsByName(search) else vm.fetchPage()
    }

    LaunchedEffect(personal) {
        if (personal) vm.fetchPersonalTrails() else vm.fetchPage()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        TopBarCreator(stringResource(Res.string.trails_title), onBack)

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 120.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SearchBar(
                search = search,
                onSearchChange = { search = it },
            )

            PersonalFilter(
                personal = personal,
                onTogglePersonal = { personal = !personal },
            )

            Spacer(Modifier.height(15.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                TrailsSection(
                    state = state,
                    vm = vm,
                    metric = metric,
                    personal = personal,
                    userRepo = userRepo,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrailsScreenPreview() =
    TrailsScreen(
        TrailFetchViewModel(FailingService, FailingService),
        {},
        SettingsViewModel(
            FailingService,
            FailingService,
        ),
        FailingService,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    search: String,
    onSearchChange: (String) -> Unit,
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
            onValueChange = onSearchChange,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalFilter(
    personal: Boolean,
    onTogglePersonal: () -> Unit,
) {
    Spacer(Modifier.height(15.dp))

    Row(
        modifier = Modifier.width(200.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilterButton(
            label = stringResource(Res.string.personal_trails_text),
            selected = personal,
            onToggle = onTogglePersonal,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UpdateTrailWarningPreview() =
    ContentWarning(
        action = Action.Trail,
        surface =
            lerp(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.primary,
                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.22f else 0.14f,
            ),
        color = MaterialTheme.colorScheme.primary,
        onSurface = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFE6EEF5) else Color(0xFF10233A),
        extraText = "Dummy",
    )

@Preview(showBackground = true)
@Composable
fun UpdateTrailButtonsPreview() =
    ContentWarningButtons(
        action = Action.Trail,
        onDismiss = {},
        onDelete = {},
        isLoading = false,
        confirmed = true,
        gradient =
            listOf(
                lerp(MaterialTheme.colorScheme.primary, Color.White, 0.18f),
                MaterialTheme.colorScheme.primary,
                lerp(MaterialTheme.colorScheme.primary, Color.Black, 0.18f),
            ),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateTrailContent(
    username: String,
    onUsernameChange: (String) -> Unit,
) {
    CustomTextField(
        value = username,
        onValueChange = onUsernameChange,
        label = Res.string.new_trail_text,
        placeholder = Res.string.trail_holder_text,
        leadingIcon = Icons.Default.Route,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(15.dp))
}

@Preview(showBackground = true)
@Composable
fun UpdateTrailWarningDialogPreview() =
    ContentWarningDialog(
        action = Action.Trail,
        isDanger = false,
        isLoading = false,
        error = null,
        onDismiss = {},
        onAction = {},
        content = {
            UpdateTrailContent(
                username = "",
                onUsernameChange = {},
            )
        },
        extraText = "Dummy",
    )

@Composable
private fun ColumnScope.TrailsColumn(
    vm: TrailFetchViewModel,
    list: List<TrailDto>,
    metric: Metric,
    personal: Boolean,
    userRepo: UserRepository,
) {
    var showUpdate by remember { mutableStateOf(false) }
    var previewTrail by remember { mutableStateOf<TrailDto?>(null) }
    val state by vm.state.collectAsState()
    var username by remember { mutableStateOf("") }
    var trailId by remember { mutableStateOf<ULong?>(null) }
    var userId by remember { mutableStateOf<ULong?>(null) }

    LaunchedEffect(Unit) {
        userId = userRepo.getOwnDetails()?.id
    }

    val error = (state as? TrailFetchState.UpdateError)?.message

    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 6.dp),
    ) {
        items(items = list, key = { it.id.toLong() }) { trail ->
            TrailCard(
                name = trail.name,
                distance = if (metric == Metric.Kilometers) trail.distance else trail.distance.toMiles(),
                difficulty = trail.difficulty,
                metric = metric,
                personal = personal || (userId != null && userId == trail.creator),
                onPreview = { previewTrail = trail },
                onUpdate = {
                    trailId = trail.id
                    showUpdate = true
                },
                onDelete = {
                    vm.deleteTrail(trail.id)
                },
            )
        }
    }

    if (showUpdate) {
        ContentWarningDialog(
            action = Action.Trail,
            isDanger = false,
            isLoading = state == TrailFetchState.Loading,
            error = error,
            onDismiss = { showUpdate = false },
            onAction = { vm.updateTrail(trailId ?: 0UL, username) },
            content = {
                UpdateTrailContent(
                    username = username,
                    onUsernameChange = { username = it },
                )
            },
        )
    }

    previewTrail?.let { trail ->
        TrailPreviewMap(
            trail = trail,
            onDismiss = { previewTrail = null },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TrailsColumnPreview() =
    Column(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        TrailsColumn(
            vm = TrailFetchViewModel(FailingService, FailingService),
            list =
                listOf(
                    TrailDto(
                        id = 1UL,
                        name = "Trail 1",
                        creator = 1UL,
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
                        creator = 2UL,
                        start = GeoPointDto(0.0, 0.0, 0.0),
                        end = GeoPointDto(0.0, 0.0, 0.0),
                        path = listOf(GeoPointDto(0.0, 0.0, 0.0)),
                        distance = 10.0,
                        difficulty = TrailDifficulty.INTERMEDIATE,
                        parent = null,
                    ),
                ),
            metric = Metric.Kilometers,
            personal = false,
            userRepo = FailingService,
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrailsSection(
    state: TrailFetchState,
    vm: TrailFetchViewModel,
    metric: Metric,
    personal: Boolean,
    userRepo: UserRepository,
) {
    Column {
        when (state) {
            TrailFetchState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator()
                }
            }

            is TrailFetchState.TrailsSuccess ->
                TrailsColumn(
                    vm = vm,
                    list = state.trails,
                    metric = metric,
                    personal = personal,
                    userRepo = userRepo,
                )

            is TrailFetchState.Error -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> {}
        }
    }
}

@Composable
fun TrailPreviewMap(
    trail: TrailDto,
    onDismiss: () -> Unit,
) {
    val path =
        remember(trail) {
            listOf(GeographicPoint(trail.start.lat, trail.start.lon, trail.start.alt)) +
                trail.path.map { GeographicPoint(it.lat, it.lon, it.alt) } +
                GeographicPoint(trail.end.lat, trail.end.lon, trail.end.alt)
        }

    MiniMapScreen(path, onDismiss)
}

fun Double.format(decimals: Int): String {
    val multiplier = 10.0.pow(decimals)
    val rounded = (this * multiplier).roundToLong()
    val sign = if (rounded < 0) "-" else ""
    val absValue = abs(rounded)
    val divisor = multiplier.toLong()
    val intPart = absValue / divisor
    val decPart = (absValue % divisor).toString().padStart(decimals, '0')
    return "$sign$intPart.$decPart"
}
