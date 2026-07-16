package pt.trekio.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtendedColors(
    val accentMuted: Color,
    val onSurfaceMuted: Color,
    val warning: Color,
)

private val LocalExtendedColors =
    staticCompositionLocalOf {
        ExtendedColors(
            accentMuted = Color.Unspecified,
            onSurfaceMuted = Color.Unspecified,
            warning = Color.Unspecified,
        )
    }

object TrekioTheme {
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}

private fun journalColorScheme() =
    lightColorScheme(
        background = JournalPalette.background,
        surface = JournalPalette.surface,
        surfaceVariant = JournalPalette.surfaceVariant,
        onBackground = JournalPalette.onBackground,
        onSurface = JournalPalette.onBackground,
        primary = JournalPalette.accent,
        onPrimary = JournalPalette.onAccent,
        outline = JournalPalette.outline,
        error = JournalPalette.danger,
    )

private fun topoColorScheme() =
    darkColorScheme(
        background = TopoPalette.background,
        surface = TopoPalette.surface,
        surfaceVariant = TopoPalette.surfaceVariant,
        onBackground = TopoPalette.onBackground,
        onSurface = TopoPalette.onBackground,
        primary = TopoPalette.accent,
        onPrimary = TopoPalette.onAccent,
        outline = TopoPalette.outline,
        error = TopoPalette.danger,
    )

@Composable
private fun ColorScheme.animated(durationMillis: Int = 400): ColorScheme {
    val spec = tween<Color>(durationMillis)
    return copy(
        background = animateColorAsState(background, spec).value,
        onBackground = animateColorAsState(onBackground, spec).value,
        surface = animateColorAsState(surface, spec).value,
        onSurface = animateColorAsState(onSurface, spec).value,
        surfaceVariant = animateColorAsState(surfaceVariant, spec).value,
        onSurfaceVariant = animateColorAsState(onSurfaceVariant, spec).value,
        primary = animateColorAsState(primary, spec).value,
        onPrimary = animateColorAsState(onPrimary, spec).value,
        outline = animateColorAsState(outline, spec).value,
        error = animateColorAsState(error, spec).value,
        onError = animateColorAsState(onError, spec).value,
    )
}

@Composable
private fun ExtendedColors.animated(durationMillis: Int = 400): ExtendedColors {
    val spec = tween<Color>(durationMillis)
    return ExtendedColors(
        accentMuted = animateColorAsState(accentMuted, spec).value,
        onSurfaceMuted = animateColorAsState(onSurfaceMuted, spec).value,
        warning = animateColorAsState(warning, spec).value,
    )
}

@Composable
fun TrekioAppTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val useDarkTheme =
        when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    val colorScheme = if (useDarkTheme) topoColorScheme() else journalColorScheme()
    val animatedColorScheme = colorScheme.animated()

    val extendedColors =
        if (useDarkTheme) {
            ExtendedColors(
                accentMuted = TopoPalette.accentMuted,
                onSurfaceMuted = TopoPalette.onSurfaceMuted,
                warning = TopoPalette.warning,
            )
        } else {
            ExtendedColors(
                accentMuted = JournalPalette.accentMuted,
                onSurfaceMuted = JournalPalette.onSurfaceMuted,
                warning = JournalPalette.warning,
            )
        }
    val animatedExtendedColors = extendedColors.animated()

    CompositionLocalProvider(LocalExtendedColors provides animatedExtendedColors) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = TrekioTypography,
            content = content,
        )
    }
}
