package pt.trekio.ui

import androidx.compose.runtime.Composable
import pt.trekio.dto.TrailDto
import pt.trekio.viewmodels.MapViewModel
import pt.trekio.viewmodels.SettingsViewModel

@Composable
expect fun MapScreen(
    viewModel: MapViewModel,
    onProfileClick: () -> Unit,
    onTrailsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onHikeClick: (TrailDto) -> Unit,
    settingsVm: SettingsViewModel,
)
