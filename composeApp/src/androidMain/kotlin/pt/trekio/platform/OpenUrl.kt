package pt.trekio.platform

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

@Composable
actual fun OpenUrl(url: String) {
    val useContext = LocalContext.current
    CustomTabsIntent.Builder().build().launchUrl(useContext, url.toUri())
}
