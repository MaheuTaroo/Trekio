package io.github.tiagopraia.kmp.mapbox.configs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class DrawingButtonsConfig(
    val columnAlignment: Alignment = Alignment.BottomStart,
    val columnModifier: Modifier = Modifier.padding(16.dp),
    val columnHorizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    val columnVerticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    val buttonsModifier: Modifier = Modifier.size(48.dp),
    val buttonsShape: Shape = RoundedCornerShape(8.dp),
    val createButtonColor: Color = Color.White,
    val createButtonImage: ImageVector = Icons.Filled.Add,
    val createButtonTint: Color = Color.Black,
    val cancelButtonColor: Color = Color.Red,
    val cancelButtonImageWithoutValues: ImageVector = Icons.Filled.Close,
    val cancelButtonImageWithValues: ImageVector = Icons.Filled.Delete,
    val cancelButtonTint: Color = Color.White,
    val undoButtonEnableColor: Color = Color.White,
    val undoButtonDisableColor: Color = Color.LightGray,
    val undoButtonImage: ImageVector = Icons.AutoMirrored.Filled.Undo,
    val undoButtonTint: Color = Color.DarkGray,
    val submitButtonEnableColor: Color = LightBlue,
    val submitButtonDisableColor: Color = Color.LightGray,
    val submitButtonImage: ImageVector = Icons.Filled.Check,
    val submitButtonTint: Color = Color.White,
)

val LightBlue = Color(0xFF3398FF)
