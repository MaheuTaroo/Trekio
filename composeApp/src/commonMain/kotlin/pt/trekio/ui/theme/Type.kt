package pt.trekio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DisplayFontFamily = FontFamily.SansSerif
val BodyFontFamily = FontFamily.Default

val TrekioTypography =
    Typography(
        // Big stat numbers — distance, elevation, duration
        displayLarge =
            TextStyle(
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                letterSpacing = (-0.5).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.25).sp,
            ),
        // Section headers
        headlineSmall =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        // Body text
        bodyLarge =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.1.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        // Captions, stat labels (the small label above a stat number)
        labelSmall =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.6.sp, // wider tracking reads as a deliberate "label", not body text
            ),
    )
