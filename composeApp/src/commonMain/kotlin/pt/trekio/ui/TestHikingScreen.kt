package pt.trekio.ui

import androidx.compose.runtime.Composable
import pt.trekio.viewmodels.TestHikingViewModel

@Composable
expect fun TestHikingScreen(vm: TestHikingViewModel)