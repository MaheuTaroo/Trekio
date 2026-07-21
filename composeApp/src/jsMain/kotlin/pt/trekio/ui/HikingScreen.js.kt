package pt.trekio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import pt.trekio.viewmodels.HikingViewModel
import pt.trekio.viewmodels.SettingsViewModel

@Composable
actual fun HikingScreen(
    vm: HikingViewModel,
    settings: SettingsViewModel,
    onStop: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text("The hiking functionality is currently not available in this version.")
    }
}
