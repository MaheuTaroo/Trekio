package pt.trekio.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

actual object LocalAppLocale {
    actual val current: String
        get() = TODO("Not yet implemented")

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        TODO("Not yet implemented")
    }
}
