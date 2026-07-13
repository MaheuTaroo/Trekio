package pt.trekio.ui.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

@get:Composable
val titleIntermediate: TextStyle
    get() =
        MaterialTheme.typography.titleLarge.copy(
            fontSize = 18.sp,
            lineHeight = 24.sp,
        )
