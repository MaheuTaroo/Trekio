package pt.trekio.ui.utils

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: StringResource,
    placeholder: StringResource,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable (() -> Unit))? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val accentColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
    )

    val iconColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 5.dp),
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            border = BorderStroke(1.dp, accentColor),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp),
                )

                Spacer(Modifier.width(10.dp))

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, accentColor),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        interactionSource = interactionSource,
                        singleLine = singleLine,
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        cursorBrush = SolidColor(accentColor),
                        textStyle =
                            MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                            ),
                        decorationBox = { innerTextField ->
                            if (value.isEmpty()) {
                                Text(
                                    text = stringResource(placeholder),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
                trailingIcon?.let {
                    Spacer(Modifier.width(5.dp))
                    it()
                }
            }
        }
    }
}
