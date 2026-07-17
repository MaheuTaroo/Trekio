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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.trekio.misc.Language
import pt.trekio.misc.Metric
import pt.trekio.services.FailingService
import pt.trekio.ui.theme.ThemeMode
import pt.trekio.ui.utils.CustomTextField
import pt.trekio.ui.utils.GradientButton
import pt.trekio.ui.utils.TopBarCreator
import pt.trekio.ui.utils.titleIntermediate
import pt.trekio.viewmodels.SettingsViewModel
import pt.trekio.viewmodels.states.SettingsState
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
import trekio.composeapp.generated.resources.kilometers_text
import trekio.composeapp.generated.resources.language_change_extended_text
import trekio.composeapp.generated.resources.language_change_text
import trekio.composeapp.generated.resources.language_metrics_text
import trekio.composeapp.generated.resources.language_text
import trekio.composeapp.generated.resources.leave_blank_text
import trekio.composeapp.generated.resources.light_mode_extended_text
import trekio.composeapp.generated.resources.light_mode_text
import trekio.composeapp.generated.resources.logout_extended_text
import trekio.composeapp.generated.resources.logout_text
import trekio.composeapp.generated.resources.metric_text
import trekio.composeapp.generated.resources.miles_text
import trekio.composeapp.generated.resources.new_password_text
import trekio.composeapp.generated.resources.new_username_text
import trekio.composeapp.generated.resources.save_changes_text
import trekio.composeapp.generated.resources.settings_text
import trekio.composeapp.generated.resources.system_based_extended_text
import trekio.composeapp.generated.resources.system_theme_text
import trekio.composeapp.generated.resources.theme_text
import trekio.composeapp.generated.resources.update_account_extended_text
import trekio.composeapp.generated.resources.update_account_text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onDelete: () -> Unit,
    settingsVm: SettingsViewModel,
) {
    var showTheme by remember { mutableStateOf(false) }
    val theme by settingsVm.theme.collectAsState()

    var showLanguage by remember { mutableStateOf(false) }
    var confirmLanguage by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf<Language?>(null) }
    val language by settingsVm.language.collectAsState()

    var showMetric by remember { mutableStateOf(false) }
    val metric by settingsVm.metric.collectAsState()

    var showUpdate by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var visiblePassword by remember { mutableStateOf(false) }

    var showLogout by remember { mutableStateOf(false) }

    var showDelete by remember { mutableStateOf(false) }
    var confirmText by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }

    val currState by settingsVm.state.collectAsState()

    LaunchedEffect(currState) {
        if (currState is SettingsState.LoggedOut) {
            onLogout()
            settingsVm.resetState()
        }
        if (currState is SettingsState.Deleted) {
            onDelete()
            settingsVm.resetState()
        }
    }

    val error = (currState as? SettingsState.Error)?.message

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

            LanguageMetricsColumn(
                onShowLanguage = { showLanguage = true },
                actualLanguage = language,
                onShowMetric = { showMetric = true },
                actualMetric = metric,
            )

            Spacer(Modifier.height(20.dp))

            AccountColumn(
                onShowUpdate = { showUpdate = true },
                onShowLogout = { showLogout = true },
                onShowDelete = { showDelete = true },
            )

            DialogAnimation(
                visible = showUpdate,
            ) {
                ContentWarningDialog(
                    action = Action.Update,
                    isDanger = false,
                    onAction = {
                        settingsVm.updateUser(
                            username = newUsername.ifEmpty { null },
                            password = newPassword.ifEmpty { null },
                        )
                        showUpdate = false
                    },
                    onDismiss = { showUpdate = false },
                    isLoading = isLoading,
                    error = error,
                ) {
                    UpdateContent(
                        username = newUsername,
                        onUsernameChange = { newUsername = it },
                        password = newPassword,
                        onPasswordChange = { newPassword = it },
                        visible = visiblePassword,
                    )
                }
            }

            DialogAnimation(
                visible = confirmLanguage,
            ) {
                ContentWarningDialog(
                    action = Action.Language,
                    isDanger = true,
                    onAction = { selectedLanguage?.let { settingsVm.setLanguage(it) } },
                    onDismiss = { confirmLanguage = false },
                    isLoading = isLoading,
                    error = error,
                )
            }

            DialogAnimation(
                visible = showLogout,
            ) {
                ContentWarningDialog(
                    action = Action.Logout,
                    isDanger = true,
                    onAction = settingsVm::logoutUser,
                    onDismiss = { showLogout = false },
                    isLoading = isLoading,
                    error = error,
                )
            }

            DialogAnimation(
                visible = showDelete,
            ) {
                ContentWarningDialog(
                    action = Action.Delete,
                    isDanger = true,
                    onAction = settingsVm::deleteUser,
                    onDismiss = { showDelete = false },
                    isLoading = isLoading,
                    error = error,
                ) {
                    DeleteContent(
                        confirmText = confirmText,
                        onConfirmTextChange = { confirmText = it },
                    )
                }
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

            if (showLanguage) {
                LanguageBottomMenu(
                    currentLanguage = language,
                    onDismiss = { showLanguage = false },
                    onLanguageSelect = {
                        selectedLanguage = it
                        confirmLanguage = true
                    },
                )
            }

            if (showMetric) {
                MetricBottomMenu(
                    currentMetric = metric,
                    onDismiss = { showMetric = false },
                    onMetricSelect = { settingsVm.setMetric(it) },
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
        settingsVm =
            SettingsViewModel(
                FailingService,
                FailingService,
            ),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    visible: Boolean,
) {
    CustomTextField(
        value = username,
        onValueChange = onUsernameChange,
        label = Res.string.new_username_text,
        placeholder = Res.string.leave_blank_text,
        leadingIcon = Icons.Default.Person,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(10.dp))

    CustomTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = Res.string.new_password_text,
        placeholder = Res.string.leave_blank_text,
        leadingIcon = Icons.Default.Lock,
        modifier = Modifier.fillMaxWidth(),
        autoComplete = true,
        visualTransformation =
            if (visible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        trailingIcon = {
            IconButton(
                onClick = { visible != visible },
                modifier = Modifier.size(20.dp),
            ) {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
    )

    Spacer(Modifier.height(15.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteContent(
    confirmText: String,
    onConfirmTextChange: (String) -> Unit,
) {
    Text(
        text = stringResource(Res.string.confirm_delete_text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(15.dp))

    CustomTextField(
        value = confirmText,
        onValueChange = onConfirmTextChange,
        label = Res.string.input_phrase,
        placeholder = Res.string.delete_button,
        leadingIcon = Icons.Outlined.Shield,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(15.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogAnimation(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
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
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionButton(
    icon: ImageVector? = null,
    text: String? = null,
    enabled: Boolean = false,
    onClick: () -> Unit = {},
    title: StringResource,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subText: StringResource? = null,
    dangerState: Boolean = false,
) {
    val useColor = if (!dangerState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = useColor,
                    )
                } else if (text != null) {
                    Text(
                        text = text,
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column {
                Text(
                    text = stringResource(title),
                    color = if (dangerState) useColor else MaterialTheme.colorScheme.onBackground,
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
private fun LanguageMetricsColumn(
    onShowLanguage: () -> Unit,
    actualLanguage: Language,
    onShowMetric: () -> Unit,
    actualMetric: Metric,
) {
    TopicColumn(
        title = Res.string.language_metrics_text,
        content = {
            OptionButton(
                text = actualLanguage.flag,
                title = Res.string.language_text,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).heightIn(min = 50.dp),
                enabled = true,
                onClick = onShowLanguage,
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Text(
                            text = actualLanguage.displayName,
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

            Spacer(Modifier.height(10.dp))

            OptionButton(
                icon = Icons.Default.Straighten,
                title = Res.string.metric_text,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).heightIn(min = 50.dp),
                enabled = true,
                onClick = onShowMetric,
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Text(
                            text =
                                when (actualMetric) {
                                    Metric.Kilometers -> stringResource(Res.string.kilometers_text)
                                    Metric.Miles -> stringResource(Res.string.miles_text)
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
fun LanguageColumnPreview() = LanguageMetricsColumn({}, Language.Portuguese, {}, Metric.Kilometers)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountColumn(
    onShowUpdate: () -> Unit,
    onShowLogout: () -> Unit,
    onShowDelete: () -> Unit,
) {
    TopicColumn(
        title = Res.string.account_text,
        content = {
            OptionButton(
                icon = Icons.Default.Update,
                title = Res.string.update_account_text,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).heightIn(min = 60.dp),
                content = {
                },
                subText = Res.string.update_account_extended_text,
                enabled = true,
                onClick = onShowUpdate,
            )

            Spacer(Modifier.height(10.dp))

            OptionButton(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = Res.string.logout_text,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).heightIn(min = 60.dp),
                content = {
                },
                subText = Res.string.logout_extended_text,
                enabled = true,
                onClick = onShowLogout,
                dangerState = true,
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
                onClick = onShowDelete,
                dangerState = true,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
fun AccountColumnPreview() = AccountColumn({}, {}, {})

private enum class Mode {
    Theme,
    Language,
    Metric,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelect(
    mode: Mode,
    icon: ImageVector? = null,
    text: String? = null,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
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
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint,
                )
            } else if (text != null) {
                Text(
                    text = text,
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )

            if (mode == Mode.Theme && subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
fun ThemeModeSelectPreview() =
    ModeSelect(
        mode = Mode.Theme,
        icon = Icons.Default.LightMode,
        iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        iconTint = MaterialTheme.colorScheme.primary,
        title = stringResource(Res.string.light_mode_text),
        subtitle = stringResource(Res.string.light_mode_extended_text),
        selected = true,
        onClick = {},
    )

@Preview(showBackground = true)
@Composable
fun LanguageModeSelectPreview() =
    ModeSelect(
        mode = Mode.Language,
        text = Language.English.flag,
        iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        iconTint = MaterialTheme.colorScheme.primary,
        title = Language.English.displayName,
        selected = true,
        onClick = {},
    )

@Preview(showBackground = true)
@Composable
fun MetricModeSelectPreview() =
    ModeSelect(
        mode = Mode.Metric,
        icon = Icons.Default.Straighten,
        iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        iconTint = MaterialTheme.colorScheme.primary,
        title = stringResource(Res.string.kilometers_text),
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
                mode = Mode.Theme,
                icon = Icons.Default.Contrast,
                iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                iconTint = MaterialTheme.colorScheme.primary,
                title = stringResource(Res.string.system_theme_text),
                subtitle = stringResource(Res.string.system_based_extended_text),
                selected = currentTheme == ThemeMode.SYSTEM,
                onClick = { onThemeSelect(ThemeMode.SYSTEM) },
            )

            ModeSelect(
                mode = Mode.Theme,
                icon = Icons.Default.LightMode,
                iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                iconTint = MaterialTheme.colorScheme.primary,
                title = stringResource(Res.string.light_mode_text),
                subtitle = stringResource(Res.string.light_mode_extended_text),
                selected = currentTheme == ThemeMode.LIGHT,
                onClick = { onThemeSelect(ThemeMode.LIGHT) },
            )

            ModeSelect(
                mode = Mode.Theme,
                icon = Icons.Default.DarkMode,
                iconBg = MaterialTheme.colorScheme.surfaceVariant,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                title = stringResource(Res.string.dark_mode_text),
                subtitle = stringResource(Res.string.dark_mode_extended_text),
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
private fun LanguageBottomMenu(
    currentLanguage: Language,
    onDismiss: () -> Unit,
    onLanguageSelect: (Language) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(Res.string.language_text),
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

            Language.entries.forEach { language ->
                ModeSelect(
                    mode = Mode.Language,
                    text = language.flag,
                    iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = language.displayName,
                    selected = currentLanguage == language,
                    onClick = { onLanguageSelect(language) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LanguageBottomMenuPreview() = LanguageBottomMenu(Language.Portuguese, {}, {})

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricBottomMenu(
    currentMetric: Metric,
    onDismiss: () -> Unit,
    onMetricSelect: (Metric) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(Res.string.metric_text),
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

            Metric.entries.forEach { metric ->
                ModeSelect(
                    mode = Mode.Metric,
                    icon = Icons.Default.Straighten,
                    iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    iconTint = MaterialTheme.colorScheme.primary,
                    title =
                        when (metric) {
                            Metric.Kilometers -> stringResource(Res.string.kilometers_text)
                            Metric.Miles -> stringResource(Res.string.miles_text)
                        },
                    selected = currentMetric == metric,
                    onClick = { onMetricSelect(metric) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MetricBottomMenuPreview() = MetricBottomMenu(Metric.Kilometers, {}, {})

private enum class Action {
    Language,
    Update,
    Logout,
    Delete,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentWarning(
    action: Action,
    surface: Color,
    color: Color,
    onSurface: Color,
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
                },
            style = MaterialTheme.typography.bodySmall,
            color = onSurface,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LanguageWarningPreview() =
    ContentWarning(
        action = Action.Language,
        surface =
            lerp(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.error,
                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.22f else 0.14f,
            ),
        color = MaterialTheme.colorScheme.error,
        onSurface = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFF5E9E6) else Color(0xFF3A1410),
    )

@Preview(showBackground = true)
@Composable
fun UpdateAccountWarningPreview() =
    ContentWarning(
        action = Action.Update,
        surface =
            lerp(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.primary,
                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.22f else 0.14f,
            ),
        color = MaterialTheme.colorScheme.primary,
        onSurface = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFE6EEF5) else Color(0xFF10233A),
    )

@Preview(showBackground = true)
@Composable
fun LogoutWarningPreview() =
    ContentWarning(
        action = Action.Logout,
        surface =
            lerp(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.error,
                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.22f else 0.14f,
            ),
        color = MaterialTheme.colorScheme.error,
        onSurface = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFF5E9E6) else Color(0xFF3A1410),
    )

@Preview(showBackground = true)
@Composable
fun DeleteAccountWarningPreview() =
    ContentWarning(
        action = Action.Delete,
        surface =
            lerp(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.error,
                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.22f else 0.14f,
            ),
        color = MaterialTheme.colorScheme.error,
        onSurface = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFF5E9E6) else Color(0xFF3A1410),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentWarningButtons(
    action: Action,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    isLoading: Boolean,
    confirmed: Boolean,
    gradient: List<Color>,
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
            enabled = (!isLoading && confirmed) || action != Action.Delete,
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
                        },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LanguageButtonsPreview() =
    ContentWarningButtons(
        action = Action.Language,
        onDismiss = {},
        onDelete = {},
        isLoading = false,
        confirmed = true,
        gradient =
            listOf(
                lerp(MaterialTheme.colorScheme.error, Color.White, 0.18f),
                MaterialTheme.colorScheme.error,
                lerp(MaterialTheme.colorScheme.error, Color.Black, 0.18f),
            ),
    )

@Preview(showBackground = true)
@Composable
fun UpdateAccountButtonsPreview() =
    ContentWarningButtons(
        action = Action.Update,
        onDismiss = {},
        onDelete = {},
        isLoading = false,
        confirmed = true,
        gradient =
            listOf(
                lerp(MaterialTheme.colorScheme.primary, Color.White, 0.18f),
                MaterialTheme.colorScheme.primary,
                lerp(MaterialTheme.colorScheme.primary, Color.Black, 0.18f),
            ),
    )

@Preview(showBackground = true)
@Composable
fun LogoutButtonsPreview() =
    ContentWarningButtons(
        action = Action.Logout,
        onDismiss = {},
        onDelete = {},
        isLoading = false,
        confirmed = true,
        gradient =
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
        action = Action.Delete,
        onDismiss = {},
        onDelete = {},
        isLoading = false,
        confirmed = true,
        gradient =
            listOf(
                lerp(MaterialTheme.colorScheme.error, Color.White, 0.18f),
                MaterialTheme.colorScheme.error,
                lerp(MaterialTheme.colorScheme.error, Color.Black, 0.18f),
            ),
    )

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
private fun ContentWarningDialog(
    action: Action,
    isDanger: Boolean,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    content: @Composable (() -> Unit)? = null,
) {
    var confirmText by remember { mutableStateOf("") }
    val confirmed = confirmText == stringResource(Res.string.delete_button)

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
                        confirmed = confirmed,
                        gradient = colors.gradient,
                    )

                    if (error != null) {
                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = error,
                            color = colors.color,
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
fun LanguageWarningDialogPreview() =
    ContentWarningDialog(
        action = Action.Language,
        isDanger = true,
        isLoading = false,
        error = null,
        onDismiss = {},
        onAction = {},
    )

@Preview(showBackground = true)
@Composable
fun UpdateAccountWarningDialogPreview() =
    ContentWarningDialog(
        action = Action.Update,
        isDanger = false,
        isLoading = false,
        error = null,
        onDismiss = {},
        onAction = {},
        content = { UpdateContent("", {}, "", {}, false) },
    )

@Preview(showBackground = true)
@Composable
fun LogoutWarningDialogPreview() =
    ContentWarningDialog(
        action = Action.Logout,
        isDanger = true,
        isLoading = false,
        error = null,
        onDismiss = {},
        onAction = {},
    )

@Preview(showBackground = true)
@Composable
fun DeleteAccountWarningDialogPreview() =
    ContentWarningDialog(
        action = Action.Delete,
        isDanger = true,
        isLoading = false,
        error = null,
        onDismiss = {},
        onAction = {},
        content = { DeleteContent("", {}) },
    )
