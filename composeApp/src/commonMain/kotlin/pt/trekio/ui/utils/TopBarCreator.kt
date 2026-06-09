package pt.trekio.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.back_arrow
import trekio.composeapp.generated.resources.back_text
import trekio.composeapp.generated.resources.dummy_text

private const val ICON_SIZE = 26

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarCreator(
    labelText: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    TopAppBar(
        title = {
            if (labelText != null) {
                Text(
                    labelText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        navigationIcon = {
            if (onBack != null) {
                Icon(
                    painter = painterResource(Res.drawable.back_arrow),
                    contentDescription = stringResource(Res.string.back_text),
                    modifier = Modifier.clickable(onClick = onBack).size(ICON_SIZE.dp),
                )
            }
        },
        actions =
            actions ?: {
                Box(modifier = Modifier.size(ICON_SIZE.dp))
            },
        colors =
            TopAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                navigationIconContentColor = Color.Gray,
                titleContentColor = Color.Gray,
                actionIconContentColor = Color.Gray,
                subtitleContentColor = Color.Transparent,
            ),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
    )
}

@Preview(showSystemUi = true)
@Composable
fun TopBarCreatorPreview() = TopBarCreator(stringResource(Res.string.dummy_text), {})
