package pt.trekio.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.cancel_text
import trekio.composeapp.generated.resources.confirm_delete_button
import trekio.composeapp.generated.resources.delete_button
import trekio.composeapp.generated.resources.language_change_extended_text
import trekio.composeapp.generated.resources.language_change_text
import trekio.composeapp.generated.resources.language_text
import trekio.composeapp.generated.resources.logout_extended_text
import trekio.composeapp.generated.resources.logout_text
import trekio.composeapp.generated.resources.oauth_dismiss_text
import trekio.composeapp.generated.resources.oauth_update_extended_text
import trekio.composeapp.generated.resources.oauth_update_text
import trekio.composeapp.generated.resources.save_changes_text
import trekio.composeapp.generated.resources.update_account_extended_text
import trekio.composeapp.generated.resources.update_account_text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentWarningDialog(
    action: Action,
    isDanger: Boolean,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    extraText: String? = null,
    confirmText: String? = null,
    enabled: Boolean = true,
    content: @Composable (() -> Unit)? = null,
) {
    val colors = rememberContentColors(isDanger)

    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ContentWarning(
                    action = action,
                    surface = colors.surface,
                    color = colors.color,
                    onSurface = colors.onSurface,
                    extraText = extraText,
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                ) {
                    content?.invoke()

                    ContentWarningButtons(
                        action = action,
                        onDismiss = onDismiss,
                        onDelete = onAction,
                        isLoading = isLoading,
                        confirmed = confirmText == stringResource(Res.string.delete_button),
                        gradient = colors.gradient,
                        enabled = enabled,
                    )

                    if (error != null) {
                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

enum class Action {
    Language,
    Update,
    Logout,
    Delete,
    OAuth,
}

private data class ContentColors(
    val color: Color,
    val gradient: List<Color>,
    val surface: Color,
    val onSurface: Color,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberContentColors(isDanger: Boolean): ContentColors {
    val color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val gradient =
        listOf(
            lerp(color, Color.White, 0.18f),
            color,
            lerp(color, Color.Black, 0.18f),
        )

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surface =
        lerp(
            MaterialTheme.colorScheme.surfaceVariant,
            color,
            if (isDark) 0.22f else 0.14f,
        )
    val onSurface =
        if (isDanger) {
            if (isDark) Color(0xFFF5E9E6) else Color(0xFF3A1410)
        } else {
            if (isDark) Color(0xFFE6EEF5) else Color(0xFF10233A)
        }

    return ContentColors(color, gradient, surface, onSurface)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentWarningButtons(
    action: Action,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    isLoading: Boolean,
    confirmed: Boolean,
    gradient: List<Color>,
    enabled: Boolean = true,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .weight(1f)
                    .height(45.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .clickable(onClick = onDismiss),
        ) {
            Text(
                text =
                    if (action != Action.OAuth) {
                        stringResource(Res.string.cancel_text)
                    } else {
                        stringResource(Res.string.oauth_dismiss_text)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        GradientButton(
            onClick = onDelete,
            enabled =
                enableContentWarningButton(
                    isLoading = isLoading,
                    confirmed = confirmed,
                    enabled = enabled,
                    action = action,
                ),
            modifier = Modifier.weight(1f).height(45.dp),
            gradientColors = gradient,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text =
                        when (action) {
                            Action.Language -> stringResource(Res.string.language_change_text)
                            Action.Update -> stringResource(Res.string.save_changes_text)
                            Action.Logout -> stringResource(Res.string.logout_text)
                            Action.Delete -> stringResource(Res.string.confirm_delete_button)
                            Action.OAuth -> stringResource(Res.string.save_changes_text)
                        },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun enableContentWarningButton(
    isLoading: Boolean,
    confirmed: Boolean,
    enabled: Boolean,
    action: Action,
) = ((!isLoading && confirmed) || action != Action.Delete) && enabled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentWarning(
    action: Action,
    surface: Color,
    color: Color,
    onSurface: Color,
    extraText: String? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(surface)
                .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(45.dp)
                    .background(Color.Transparent, CircleShape)
                    .border(1.5.dp, color, CircleShape),
        ) {
            Icon(
                imageVector =
                    when (action) {
                        Action.Language -> Icons.Default.Language
                        Action.Update -> Icons.Default.Person
                        Action.Logout -> Icons.AutoMirrored.Default.Logout
                        Action.Delete -> Icons.Default.Delete
                        Action.OAuth -> Icons.Default.Check
                    },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text =
                when (action) {
                    Action.Language -> stringResource(Res.string.language_text)
                    Action.Update -> stringResource(Res.string.update_account_text)
                    Action.Logout -> stringResource(Res.string.logout_text)
                    Action.Delete -> stringResource(Res.string.delete_button)
                    Action.OAuth -> stringResource(Res.string.oauth_update_text)
                },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text =
                when (action) {
                    Action.Language -> stringResource(Res.string.language_change_extended_text)
                    Action.Update -> stringResource(Res.string.update_account_extended_text)
                    Action.Logout -> stringResource(Res.string.logout_extended_text)
                    Action.Delete -> stringResource(Res.string.logout_extended_text)
                    Action.OAuth -> stringResource(Res.string.oauth_update_extended_text, extraText ?: "")
                },
            style = MaterialTheme.typography.bodySmall,
            color = onSurface,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
        )
    }
}
