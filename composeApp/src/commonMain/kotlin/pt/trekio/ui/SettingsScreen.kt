package pt.trekio.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pt.trekio.services.FailingService
import pt.trekio.ui.theme.ThemeMode
import pt.trekio.ui.utils.GradientButton
import pt.trekio.ui.utils.TopBarCreator
import pt.trekio.ui.utils.titleIntermediate
import pt.trekio.viewmodels.SettingsViewModel
import pt.trekio.viewmodels.UserProfileViewModel
import pt.trekio.viewmodels.states.UserProfileState
import trekio.composeapp.generated.resources.Res
import trekio.composeapp.generated.resources.account_text
import trekio.composeapp.generated.resources.appearance_text
import trekio.composeapp.generated.resources.assurance_text
import trekio.composeapp.generated.resources.confirm_delete_button
import trekio.composeapp.generated.resources.confirm_delete_text
import trekio.composeapp.generated.resources.dark_mode_text
import trekio.composeapp.generated.resources.delete_account_extended_text
import trekio.composeapp.generated.resources.delete_button
import trekio.composeapp.generated.resources.input_phrase
import trekio.composeapp.generated.resources.language_metrics_text
import trekio.composeapp.generated.resources.language_text
import trekio.composeapp.generated.resources.light_mode_text
import trekio.composeapp.generated.resources.logout_extended_text
import trekio.composeapp.generated.resources.logout_text
import trekio.composeapp.generated.resources.settings_text
import trekio.composeapp.generated.resources.system_theme_text
import trekio.composeapp.generated.resources.theme_text
import trekio.composeapp.generated.resources.warning_text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onDelete: () -> Unit,
    userVm: UserProfileViewModel,
    settingsVm: SettingsViewModel,
) {
    var showDelete by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    val theme by settingsVm.theme.collectAsState()

    val currState by userVm.state.collectAsState()

    LaunchedEffect(currState) {
        if (currState is UserProfileState.Deleted) onDelete()
    }

    val error = (currState as? UserProfileState.Error)?.message

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        TopBarCreator(stringResource(Res.string.settings_text), onBack)

        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(top = 110.dp),
        ) {
            AppearanceColumn(
                onShowTheme = { showTheme = true },
                actualTheme = theme.name,
                showTheme = showTheme,
            )

            Spacer(Modifier.height(20.dp))

            TopicColumn(
                title = Res.string.language_metrics_text,
                content = {
                    OptionButton(
                        icon = Icons.Default.Language,
                        title = Res.string.language_text,
                        modifier = Modifier.width(300.dp).height(50.dp),
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
                        modifier = Modifier.width(300.dp).height(50.dp),
                        content = {
                        },
                        subText = Res.string.logout_extended_text,
                        enabled = true,
                        onClick = onLogout,
                        changeUserState = true,
                    )

                    Spacer(Modifier.height(10.dp))

                    OptionButton(
                        icon = Icons.Default.Delete,
                        title = Res.string.delete_button,
                        modifier = Modifier.width(300.dp).height(50.dp),
                        content = {
                        },
                        subText = Res.string.delete_account_extended_text,
                        enabled = true,
                        onClick = { showDelete = true },
                        changeUserState = true,
                    )
                },
            )

            if (showDelete) {
                DeleteAccountDialog(
                    onDelete = onDelete,
                    onDismiss = { showDelete = false },
                    isLoading = isLoading,
                    error = error,
                )
            }

            if (showTheme) {
                ThemeBottomMenu(
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
private fun TopicColumn(
    title: StringResource,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = stringResource(title),
            style = titleIntermediate,
            textAlign = TextAlign.Left,
            modifier = Modifier.width(300.dp).padding(start = 5.dp),
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(5.dp))

        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceColumn(
    onShowTheme: () -> Unit,
    actualTheme: String,
    showTheme: Boolean,
) {
    TopicColumn(
        title = Res.string.appearance_text,
        content = {
            OptionButton(
                icon =
                    when (actualTheme) {
                        "DARK" -> Icons.Default.DarkMode
                        "LIGHT" -> Icons.Default.LightMode
                        else -> Icons.Default.Phone
                    },
                title = Res.string.theme_text,
                modifier = Modifier.width(300.dp).height(50.dp),
                content = {
                    Button(
                        onClick = onShowTheme,
                        modifier = Modifier.height(30.dp).padding(end = 10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = actualTheme,
                                style = MaterialTheme.typography.bodySmall,
                            )

                            Spacer(Modifier.width(5.dp))

                            Icon(
                                imageVector = if (!showTheme) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
            )
        },
    )
}

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
                    style = MaterialTheme.typography.titleMedium,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeBottomMenu(
    onDismiss: () -> Unit,
    onThemeSelect: (ThemeMode) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column {
            ModeSelect(
                title = Res.string.system_theme_text,
                onClick = { onThemeSelect(ThemeMode.SYSTEM) },
            )

            Spacer(Modifier.height(5.dp))

            ModeSelect(
                title = Res.string.light_mode_text,
                onClick = { onThemeSelect(ThemeMode.LIGHT) },
            )

            Spacer(Modifier.height(5.dp))

            ModeSelect(
                title = Res.string.dark_mode_text,
                onClick = { onThemeSelect(ThemeMode.DARK) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelect(
    title: StringResource,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
    ) {
        Text(
            text = stringResource(title),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteAccountDialog(
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmText by remember { mutableStateOf("") }

    val errorColor = MaterialTheme.colorScheme.error
    val errorGradient =
        listOf(
            lerp(errorColor, Color.White, 0.18f),
            errorColor,
            lerp(errorColor, Color.Black, 0.18f),
        )

    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .width(320.dp)
                        .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.warning_text),
                        style = titleIntermediate,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(5.dp))

                Text(
                    stringResource(Res.string.assurance_text),
                    style = titleIntermediate,
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    stringResource(Res.string.confirm_delete_text),
                    style = titleIntermediate,
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it },
                    modifier = Modifier.width(200.dp),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.input_phrase)) },
                )

                Spacer(Modifier.height(15.dp))

                GradientButton(
                    onClick = onDelete,
                    enabled = !isLoading && confirmText == stringResource(Res.string.delete_button),
                    modifier = Modifier.width(100.dp),
                    gradientColors = errorGradient,
                ) {
                    Text(
                        text = stringResource(Res.string.confirm_delete_button),
                        style = titleIntermediate,
                    )
                }

                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = titleIntermediate,
                        modifier = Modifier.width(200.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
