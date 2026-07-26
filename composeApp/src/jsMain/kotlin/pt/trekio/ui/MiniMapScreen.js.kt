package pt.trekio.ui

import androidx.compose.runtime.Composable
import io.github.tiagopraia.kmp.mapbox.GeographicPoint

@Composable
actual fun MiniMapScreen(
    path: List<GeographicPoint>,
    onDismiss: () -> Unit,
) {}
