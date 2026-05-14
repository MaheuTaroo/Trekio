package pt.trekio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pt.trekio.ui.utils.GradientButton
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.auth_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleScreen(
    onAuthenticateClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "(Logo for application)",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.padding(top = 36.dp))

        Text(
            text = "(Some Intro text for the application info)",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.padding(top = 24.dp))

        Text(
            text = "(Developers maybe)",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.padding(top = 36.dp))

        GradientButton(
            onClick = onAuthenticateClick,
            modifier = Modifier.width(120.dp),
        ) {
            Text(stringResource(Res.string.auth_title))
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun TitleScreenPreview() = TitleScreen({})
