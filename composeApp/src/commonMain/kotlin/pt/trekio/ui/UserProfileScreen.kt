package pt.trekio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.trekio.ui.utils.DataCard
import pt.trekio.ui.utils.GradientButton
import pt.trekio.ui.utils.TopBarCreator
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.delete_button
import trekio.composeapp.generated.resources.email_text
import trekio.composeapp.generated.resources.total_km_text
import trekio.composeapp.generated.resources.total_trails_text
import trekio.composeapp.generated.resources.update_button
import trekio.composeapp.generated.resources.user_profile_title
import trekio.composeapp.generated.resources.username_text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(onBack: () -> Unit) {
    var openUpdate by remember { mutableStateOf(false) }
    var openDelete by remember { mutableStateOf(false) }

    TopBarCreator(stringResource(Res.string.user_profile_title), onBack)

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(top = 100.dp),
    ) {
        DataCard(Res.string.username_text, "")

        Spacer(Modifier.height(20.dp))

        DataCard(Res.string.email_text, "")

        Spacer(Modifier.height(20.dp))

        DataCard(Res.string.total_trails_text, "")

        Spacer(Modifier.height(20.dp))

        DataCard(Res.string.total_km_text, "")

        Spacer(Modifier.height(30.dp))

        GradientButton(
            onClick = { openUpdate = !openUpdate },
            modifier = Modifier.width(200.dp),
        ) {
            Text(
                text = stringResource(Res.string.update_button),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        if (openUpdate) {
            TODO("Dropdown")
        }

        Spacer(Modifier.height(30.dp))

        GradientButton(
            onClick = { openDelete = !openDelete },
            modifier = Modifier.width(200.dp),
            gradientColors =
                listOf(
                    Color(0xFFE54747),
                    Color(0xFFAA0000),
                ),
        ) {
            Text(
                text = stringResource(Res.string.delete_button),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        if (openDelete) {
            TODO("Dropdown")
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun UserProfileScreenPreview() = UserProfileScreen({})
