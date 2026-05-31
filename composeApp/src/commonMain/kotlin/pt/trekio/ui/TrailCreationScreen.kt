package pt.trekio.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.trekio.ui.utils.TopBarCreator
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.trail_creation_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailCreationScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
) {
    TopBarCreator(stringResource(Res.string.trail_creation_title), onBack)

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 80.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
    }
}
