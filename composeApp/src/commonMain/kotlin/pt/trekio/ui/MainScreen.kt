package pt.trekio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tiagopraia.kmp.mapbox.Map
import io.github.tiagopraia.kmp.mapbox.MapOverlayAction
import io.github.tiagopraia.kmp.mapbox.MapViewModel
import io.github.tiagopraia.kmp.mapbox.configs.MapConfig
import io.github.tiagopraia.kmp.mapbox.configs.MapStyle
import pt.trekio.platformConfig

@Composable
fun MainScreen(
    onProfileClick: () -> Unit,
    onTrailsClick: () -> Unit,
    mapViewModel: MapViewModel,
) {
    val extraOverlays =
        listOf(
            MapOverlayAction {
                Box(modifier = Modifier.fillMaxWidth()) { ProfileButton(onProfileClick) }
            },
            MapOverlayAction {
                Box(modifier = Modifier.fillMaxSize()) { TrailsButton(onTrailsClick) }
            },
        )
    Map(
        accessToken = platformConfig(),
        vm = mapViewModel,
        config =
            MapConfig(
                styleUri = if (isSystemInDarkTheme()) MapStyle.DARK else MapStyle.OUTDOORS,
            ),
        extraOverlays = extraOverlays,
    )
}

@Composable
fun BoxScope.ProfileButton(onProfileClick: () -> Unit) {
    FloatingActionButton(
        onClick = { onProfileClick() },
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 50.dp, end = 16.dp)
                .size(40.dp),
        shape = CircleShape,
        containerColor = Color.White,
        content = {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Start route creation",
                tint = Color.Black,
            )
        },
    )
}

@Composable
fun BoxScope.TrailsButton(onTrailsClick: () -> Unit) {
    FloatingActionButton(
        onClick = { onTrailsClick() },
        modifier =
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp + 16.dp + 8.dp, bottom = 16.dp) // size + padding + spaceBetween
                .size(48.dp),
        shape = RoundedCornerShape(8.dp),
        containerColor = Color.White,
        content = {
            Text("Trails")
        },
    )
}

@Preview
@Composable
fun MainScreenPreview() =
    MainScreen(
        {},
        {},
        viewModel<MapViewModel>(factory = MapViewModel.getFactory()),
    )
