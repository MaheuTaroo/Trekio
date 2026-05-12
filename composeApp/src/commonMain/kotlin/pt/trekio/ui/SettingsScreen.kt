package pt.trekio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.trekio.ui.utils.SettingsDropdown
import pt.trekio.ui.utils.TopBarCreator
import pt.trekio.viewmodels.Language
import pt.trekio.viewmodels.Theme
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.settings_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    var selectedTheme by remember { mutableStateOf("System Based") }
    var selectedLanguage by remember { mutableStateOf("English") }

    TopBarCreator(stringResource(Res.string.settings_title), onBack)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SettingsDropdown(
            "Theme",
            Theme.entries.map { it.value },
            selectedTheme,
            { selectedTheme = it }, /* TODO */
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsDropdown(
            title = "Language",
            options = Language.entries.map { it.value },
            selectedLanguage,
            { selectedLanguage = it }, /* TODO */
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview
@Composable
fun SettingsScreenPreview() =
    SettingsScreen(onBack = {})