package pt.trekio.ui

import androidx.compose.runtime.Composable
import pt.trekio.viewmodels.HikingViewModel

@Composable
expect fun HikingScreen(
    vm: HikingViewModel,
    onStop: () -> Unit,
)
