package pt.trekio.misc

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.stats_time_format_h_m_s
import trekio.composeapp.generated.resources.stats_time_format_m_s
import trekio.composeapp.generated.resources.stats_time_format_s
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

@Composable
fun Long.formatAsTime(): String =
    when {
        this >= 3600 -> { // h m s
            val hours = this / 3600
            val minutes = (this / 60) % 60
            val seconds = this % 60
            stringResource(Res.string.stats_time_format_h_m_s, hours, minutes, seconds)
        }
        this > 60 -> { // m s
            val minutes = (this / 60) % 60
            val seconds = this % 60
            stringResource(Res.string.stats_time_format_m_s, minutes, seconds)
        }
        else -> stringResource(Res.string.stats_time_format_s, this % 60) // s
    }

fun Double.format(decimals: Int): String {
    val multiplier = 10.0.pow(decimals)
    val rounded = (this * multiplier).roundToLong()
    val sign = if (rounded < 0) "-" else ""
    val absValue = abs(rounded)
    val divisor = multiplier.toLong()
    val intPart = absValue / divisor
    val decPart = (absValue % divisor).toString().padStart(decimals, '0')
    return "$sign$intPart.$decPart"
}
