package pt.trekio.platform

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import java.util.Locale

actual object LocalAppLocale {
    private var default: Locale? = null

    actual val current: String
        @Composable get() = LocalLocale.current.platformLocale.toLanguageTag()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val config = LocalConfiguration.current
        if (default == null) default = LocalLocale.current.platformLocale

        val new = if (value == null) default!! else Locale.forLanguageTag(value)
        Locale.setDefault(new)
        config.setLocale(new)

        val resources: Resources = LocalResources.current
        resources.updateConfiguration(config, resources.displayMetrics)

        return LocalConfiguration.provides(config)
    }
}
