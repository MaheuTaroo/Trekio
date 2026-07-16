package pt.trekio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.trekio.services.FailingService
import pt.trekio.ui.theme.ThemeMode
import pt.trekio.ui.utils.CustomTextField
import pt.trekio.ui.utils.GradientButton
import pt.trekio.ui.utils.TopBarCreator
import pt.trekio.ui.utils.titleIntermediate
import pt.trekio.viewmodels.SettingsViewModel
import pt.trekio.viewmodels.UserProfileViewModel
import pt.trekio.viewmodels.states.UserProfileState
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.account_text
import trekio.composeapp.generated.resources.appearance_text
import trekio.composeapp.generated.resources.cancel_text
import trekio.composeapp.generated.resources.confirm_delete_button
import trekio.composeapp.generated.resources.confirm_delete_text
import trekio.composeapp.generated.resources.dark_mode_extended_text
import trekio.composeapp.generated.resources.dark_mode_text
import trekio.composeapp.generated.resources.delete_account_extended_text
import trekio.composeapp.generated.resources.delete_button
import trekio.composeapp.generated.resources.dummy_text
import trekio.composeapp.generated.resources.input_phrase
import trekio.composeapp.generated.resources.language_metrics_text
import trekio.composeapp.generated.resources.language_text
import trekio.composeapp.generated.resources.light_mode_extended_text
import trekio.composeapp.generated.resources.light_mode_text
import trekio.composeapp.generated.resources.logout_extended_text
import trekio.composeapp.generated.resources.logout_text
import trekio.composeapp.generated.resources.settings_text
import trekio.composeapp.generated.resources.system_based_extended_text
import trekio.composeapp.generated.resources.system_theme_text
import trekio.composeapp.generated.resources.theme_text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onDelete: () -> Unit,
    userVm: UserProfileViewModel,
    settingsVm: SettingsViewModel,
) {
    var showLogout by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    val theme by settingsVm.theme.collectAsState()

    val currState by userVm.state.collectAsState()

    LaunchedEffect(currState) {
        if (currState is UserProfileState.Deleted) onDelete()
    }

    LaunchedEffect(currState) {
        if (currState is UserProfileState.LoggedOut) onLogout()
    }

    val error = (currState as? UserProfileState.Error)?.message

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        TopBarCreator(stringResource(Res.string.settings_text), onBack)

        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(top = 120.dp),
        ) {
            AppearanceColumn(
                onShowTheme = { showTheme = true },
                actualTheme = theme.name,
            )

            Spacer(Modifier.height(20.dp))

            TopicColumn(
                title = Res.string.language_metrics_text,
                content = {
                    OptionButton(
                        icon = Icons.Default.Language,
                        title = Res.string.language_text,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).heightIn(min = 50.dp),
                        content = {
                        },
                    )
                },
            )

            Spacer(Modifier.height(20.dp))

            TopicColumn(
                title = Res.string.account_text,
                content = {
                    OptionButton(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = Res.string.logout_text,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).heightIn(min = 60.dp),
                        content = {
                        },
                        subText = Res.string.logout_extended_text,
                        enabled = true,
                        onClick = { showLogout = true },
                        changeUserState = true,
                    )

                    Spacer(Modifier.height(10.dp))

                    OptionButton(
                        icon = Icons.Default.Delete,
                        title = Res.string.delete_button,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).heightIn(min = 60.dp),
                        content = {
                        },
                        subText = Res.string.delete_account_extended_text,
                        enabled = true,
                        onClick = { showDelete = true },
                        changeUserState = true,
                    )
                },
            )

            AnimatedVisibility(
                visible = showLogout,
                enter =
                    fadeIn() +
                        scaleIn(
                            initialScale = 0.8f,
                        ),
                exit =
                    fadeOut() +
                        scaleOut(
                            targetScale = 0.8f,
                        ),
            ) {
                ContentWarningDialog(
                    isLogout = true,
                    onDelete = onLogout,
                    onDismiss = { showLogout = false },
                    isLoading = isLoading,
                    error = error,
                )
            }

            AnimatedVisibility(
                visible = showDelete,
                enter =
                    fadeIn() +
                        scaleIn(
                            initialScale = 0.8f,
                        ),
                exit =
                    fadeOut() +
                        scaleOut(
                            targetScale = 0.8f,
                        ),
            ) {
                ContentWarningDialog(
                    isLogout = false,
                    onDelete = onDelete,
                    onDismiss = { showDelete = false },
                    isLoading = isLoading,
                    error = error,
                )
            }

            if (showTheme) {
                ThemeBottomMenu(
                    currentTheme = theme,
                    onDismiss = {
                        showTheme = false
                    },
                    onThemeSelect = { settingsVm.setThemeMode(it) },
                )
            }
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() =
    SettingsScreen(
        onBack = {},
        onLogout = {},
        onDelete = {},
        userVm = UserProfileViewModel(FailingService),
        SettingsViewModel(
            FailingService,
        ),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionButton(
    icon: ImageVector,
    enabled: Boolean = false,
    onClick: () -> Unit = {},
    title: StringResource,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subText: StringResource? = null,
    changeUserState: Boolean = false,
) {
    val useColor = if (!changeUserState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
            modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(10.dp),
                ).clickable(
                    enabled = enabled,
                    onClick = onClick,
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.padding(start = 10.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(30.dp).background(useColor.copy(alpha = 0.25f), RoundedCornerShape(5.dp)),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = useColor,
                )
            }

            Spacer(Modifier.width(10.dp))

            Column {
                Text(
                    text = stringResource(title),
                    color = if (changeUserState) useColor else MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleSmall,
                )

                subText?.let {
                    Text(
                        text = stringResource(subText),
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OptionButtonPreview() =
    OptionButton(
        icon = Icons.Default.Done,
        title = Res.string.dummy_text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).heightIn(min = 50.dp),
        content = {},
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicColumn(
    title: StringResource,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = stringResource(title),
            style = titleIntermediate,
            textAlign = TextAlign.Left,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).padding(start = 5.dp),
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(5.dp))

        content()
    }
}

@Preview(showBackground = true)
@Composable
fun TopicColumnPreview() =
    TopicColumn(
        title = Res.string.dummy_text,
    ) {
        OptionButton(
            icon = Icons.Default.Done,
            title = Res.string.dummy_text,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).heightIn(min = 50.dp),
            content = {},
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceColumn(
    onShowTheme: () -> Unit,
    actualTheme: String,
) {
    TopicColumn(
        title = Res.string.appearance_text,
        content = {
            OptionButton(
                icon =
                    when (actualTheme) {
                        "DARK" -> Icons.Default.DarkMode
                        "LIGHT" -> Icons.Default.LightMode
                        else -> Icons.Default.Contrast
                    },
                title = Res.string.theme_text,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).heightIn(min = 50.dp),
                enabled = true,
                onClick = onShowTheme,
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Text(
                            text =
                                when (actualTheme) {
                                    "DARK" -> stringResource(Res.string.dark_mode_text)
                                    "LIGHT" -> stringResource(Res.string.light_mode_text)
                                    else -> stringResource(Res.string.system_theme_text)
                                },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(Modifier.width(5.dp))

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
fun AppearanceColumnPreview() = AppearanceColumn({}, stringResource(Res.string.system_theme_text))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelect(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: StringResource,
    subtitle: StringResource,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                ).clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(30.dp).background(iconBg, RoundedCornerShape(10.dp)),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconTint,
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(title),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )

            Text(
                text = stringResource(subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ModeSelectPreview() =
    ModeSelect(
        icon = Icons.Default.Done,
        iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        iconTint = MaterialTheme.colorScheme.primary,
        title = Res.string.dummy_text,
        subtitle = Res.string.dummy_text,
        selected = true,
        onClick = {},
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeBottomMenu(
    currentTheme: ThemeMode,
    onDismiss: () -> Unit,
    onThemeSelect: (ThemeMode) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(Res.string.appearance_text),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            )

            Spacer(Modifier.height(15.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                thickness = 0.5.dp,
            )

            Spacer(Modifier.height(10.dp))

            ModeSelect(
                icon = Icons.Default.Contrast,
                iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                iconTint = MaterialTheme.colorScheme.primary,
                title = Res.string.system_theme_text,
                subtitle = Res.string.system_based_extended_text,
                selected = currentTheme == ThemeMode.SYSTEM,
                onClick = { onThemeSelect(ThemeMode.SYSTEM) },
            )

            ModeSelect(
                icon = Icons.Default.LightMode,
                iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                iconTint = MaterialTheme.colorScheme.primary,
                title = Res.string.light_mode_text,
                subtitle = Res.string.light_mode_extended_text,
                selected = currentTheme == ThemeMode.LIGHT,
                onClick = { onThemeSelect(ThemeMode.LIGHT) },
            )

            ModeSelect(
                icon = Icons.Default.DarkMode,
                iconBg = MaterialTheme.colorScheme.surfaceVariant,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                title = Res.string.dark_mode_text,
                subtitle = Res.string.dark_mode_extended_text,
                selected = currentTheme == ThemeMode.DARK,
                onClick = { onThemeSelect(ThemeMode.DARK) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ThemeBottomMenuPreview() = ThemeBottomMenu(ThemeMode.SYSTEM, {}, {})

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentWarning(
    isLogout: Boolean,
    dangerSurface: Color,
    errorColor: Color,
    onDangerSurface: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(dangerSurface)
                .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(45.dp)
                    .background(Color.Transparent, CircleShape)
                    .border(1.5.dp, errorColor, CircleShape),
        ) {
            Icon(
                imageVector = if (isLogout) Icons.AutoMirrored.Default.Logout else Icons.Default.Delete,
                contentDescription = null,
                tint = errorColor,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = if (isLogout) stringResource(Res.string.logout_text) else stringResource(Res.string.delete_button),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = errorColor,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text =
                if (isLogout) {
                    stringResource(
                        Res.string.logout_extended_text,
                    )
                } else {
                    stringResource(Res.string.delete_account_extended_text)
                },
            style = MaterialTheme.typography.bodySmall,
            color = onDangerSurface,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LogoutWarningPreview() =
    ContentWarning(
        isLogout = true,
        dangerSurface =
            lerp(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.error,
                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.22f else 0.14f,
            ),
        errorColor = MaterialTheme.colorScheme.error,
        onDangerSurface = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFF5E9E6) else Color(0xFF3A1410),
    )

@Preview(showBackground = true)
@Composable
fun DeleteAccountWarningPreview() =
    ContentWarning(
        isLogout = false,
        dangerSurface =
            lerp(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.error,
                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.22f else 0.14f,
            ),
        errorColor = MaterialTheme.colorScheme.error,
        onDangerSurface = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFF5E9E6) else Color(0xFF3A1410),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentWarningButtons(
    isLogout: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    isLoading: Boolean,
    confirmed: Boolean,
    errorGradient: List<Color>,
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
                text = stringResource(Res.string.cancel_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        GradientButton(
            onClick = onDelete,
            enabled = (!isLoading && confirmed) || isLogout,
            modifier = Modifier.weight(1f).height(45.dp),
            gradientColors = errorGradient,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = if (isLogout) stringResource(Res.string.logout_text) else stringResource(Res.string.confirm_delete_button),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LogoutButtonsPreview() =
    ContentWarningButtons(
        isLogout = true,
        onDismiss = {},
        onDelete = {},
        isLoading = false,
        confirmed = true,
        errorGradient =
            listOf(
                lerp(MaterialTheme.colorScheme.error, Color.White, 0.18f),
                MaterialTheme.colorScheme.error,
                lerp(MaterialTheme.colorScheme.error, Color.Black, 0.18f),
            ),
    )

@Preview(showBackground = true)
@Composable
fun DeleteAccountButtonsPreview() =
    ContentWarningButtons(
        isLogout = false,
        onDismiss = {},
        onDelete = {},
        isLoading = false,
        confirmed = true,
        errorGradient =
            listOf(
                lerp(MaterialTheme.colorScheme.error, Color.White, 0.18f),
                MaterialTheme.colorScheme.error,
                lerp(MaterialTheme.colorScheme.error, Color.Black, 0.18f),
            ),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentWarningDialog(
    isLogout: Boolean,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmText by remember { mutableStateOf("") }
    val confirmPhrase = stringResource(Res.string.delete_button)
    val confirmed = confirmText == confirmPhrase

    val errorColor = MaterialTheme.colorScheme.error
    val errorGradient =
        listOf(
            lerp(errorColor, Color.White, 0.18f),
            errorColor,
            lerp(errorColor, Color.Black, 0.18f),
        )

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dangerSurface =
        lerp(
            MaterialTheme.colorScheme.surfaceVariant,
            errorColor,
            if (isDark) 0.22f else 0.14f,
        )
    val onDangerSurface = if (isDark) Color(0xFFF5E9E6) else Color(0xFF3A1410)

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
                    isLogout = isLogout,
                    dangerSurface = dangerSurface,
                    errorColor = errorColor,
                    onDangerSurface = onDangerSurface,
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                ) {
                    if (!isLogout) {
                        Text(
                            text = stringResource(Res.string.confirm_delete_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(15.dp))

                        CustomTextField(
                            value = confirmText,
                            onValueChange = { confirmText = it },
                            label = Res.string.input_phrase,
                            placeholder = Res.string.delete_button,
                            leadingIcon = Icons.Outlined.Shield,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(15.dp))
                    }

                    ContentWarningButtons(
                        isLogout = isLogout,
                        onDismiss = onDismiss,
                        onDelete = onDelete,
                        isLoading = isLoading,
                        confirmed = confirmed,
                        errorGradient = errorGradient,
                    )

                    if (error != null) {
                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = error,
                            color = errorColor,
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

@Preview(showBackground = true)
@Composable
fun LogoutDialogPreview() =
    ContentWarningDialog(
        isLogout = true,
        isLoading = false,
        error = null,
        onDismiss = {},
        onDelete = {},
    )

@Preview(showBackground = true)
@Composable
fun DeleteAccountDialogPreview() =
    ContentWarningDialog(
        isLogout = false,
        isLoading = false,
        error = null,
        onDismiss = {},
        onDelete = {},
    )
