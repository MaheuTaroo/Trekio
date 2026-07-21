package pt.trekio.ui

import androidx.compose.runtime.Composable
import pt.trekio.viewmodels.HikingViewModel
import pt.trekio.viewmodels.SettingsViewModel

@Composable
expect fun HikingScreen(
    vm: HikingViewModel,
    settings: SettingsViewModel,
    onStop: () -> Unit,
)
