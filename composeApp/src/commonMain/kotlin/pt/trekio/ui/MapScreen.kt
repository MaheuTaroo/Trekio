package pt.trekio.ui

import androidx.compose.runtime.Composable
import pt.trekio.viewmodels.MapViewModel

@Composable
expect fun MapScreen(
    viewModel: MapViewModel,
    onProfileClick: () -> Unit,
    onTrailsClick: () -> Unit,
)
