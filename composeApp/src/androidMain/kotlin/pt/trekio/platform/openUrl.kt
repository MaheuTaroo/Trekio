package pt.trekio.platform

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun openUrl(url: String) {
    val useContext = LocalContext.current
    CustomTabsIntent.Builder().build().launchUrl(useContext, Uri.parse(url))
}