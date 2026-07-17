package pt.trekio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.tiagopraia.kmp.mapbox.config.AndroidFollowButtonConfig
import pt.trekio.R
import pt.trekio.ui.theme.ThemeMode
import pt.trekio.ui.theme.TrekioAppTheme
import pt.trekio.ui.utils.CustomTextField
import pt.trekio.ui.utils.Option
import pt.trekio.ui.utils.OverlayMenuButtons
import pt.trekio.viewmodels.states.TrailState

@Composable
fun BoxScope.MapOverlayButtons(
    followButtonConfig: AndroidFollowButtonConfig,
    isDrawingMode: Boolean,
    canUndo: Boolean,
    canComplete: Boolean,
    hasCompleted: Boolean,
    routeName: String,
    trailState: TrailState,
    onRouteNameChange: (String) -> Unit,
    onProfileClick: () -> Unit,
    onTrailsClick: () -> Unit,
    onStartRoute: () -> Unit,
    onUndo: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
    onCommit: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    val drawingConfig = remember { DrawingButtonsConfig() }

    OverlayMenuButtons(
        config = followButtonConfig,
        options =
            listOf(
                Option(
                    icon = Icons.Default.Person,
                    label = stringResource(R.string.profile_title),
                    angle = 270f,
                    onClick = { onProfileClick() },
                ),
                Option(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = stringResource(R.string.logout_title),
                    angle = 30f,
                    onClick = { onLogout() },
                ),
                Option(
                    icon = Icons.Default.Settings,
                    label = stringResource(R.string.settings_title),
                    angle = 150f,
                    onClick = { onSettings() },
                ),
            ),
        modifier = Modifier,
    )
    TrailsButton(drawingConfig, onTrailsClick)
    if (isDrawingMode) {
        DrawingButtons(
            config = drawingConfig,
            canUndo = canUndo,
            canComplete = canComplete,
            onUndo = onUndo,
            onCancel = onCancel,
            onComplete = onComplete,
        )
    } else {
        CreationButton(
            config = drawingConfig,
            onClick = onStartRoute,
        )
    }

    if (hasCompleted) {
        RouteCommitDialog(
            routeName = routeName,
            commitError = (trailState as? TrailState.Error)?.message,
            isCommitLoading = trailState is TrailState.Loading,
            onRouteNameChange = onRouteNameChange,
            onCancel = onCancel,
            onCommit = onCommit,
        )
    }
}

@Composable
private fun RouteCommitDialog(
    routeName: String,
    commitError: String?,
    isCommitLoading: Boolean,
    onRouteNameChange: (String) -> Unit,
    onCancel: () -> Unit,
    onCommit: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                CustomTextField(
                    value = routeName,
                    onValueChange = onRouteNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = Icons.Default.Route,
                    labelText = stringResource(R.string.name_text),
                    placeholderText = stringResource(R.string.name_text),
                )

                if (commitError != null) {
                    Text(
                        text = commitError,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    TextButton(
                        onClick = onCancel,
                        enabled = !isCommitLoading,
                    ) {
                        Text(stringResource(R.string.cancel_text))
                    }

                    Button(
                        onClick = onCommit,
                        enabled = !isCommitLoading && routeName.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.create_text))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RouteCommitDialogPreview() = TrekioAppTheme(themeMode = ThemeMode.LIGHT) { RouteCommitDialog("Trail 1", null, false, {}, {}, {}) }

@Composable
fun BoxScope.CreationButton(
    config: DrawingButtonsConfig,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.align(config.columnAlignment).then(config.columnModifier),
        horizontalAlignment = config.columnHorizontalAlignment,
        verticalArrangement = config.columnVerticalArrangement,
    ) {
        FloatingActionButton(
            onClick = onClick,
            shape = config.buttonsShape,
            containerColor = config.createButtonColor,
            modifier = config.buttonsModifier,
        ) {
            Icon(
                imageVector = config.createButtonImage,
                contentDescription = "Start route creation",
                tint = config.createButtonTint,
            )
        }
    }
}

@Composable
private fun BoxScope.DrawingButtons(
    config: DrawingButtonsConfig,
    canUndo: Boolean,
    canComplete: Boolean,
    onUndo: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
) {
    Column(
        modifier = Modifier.align(config.columnAlignment).then(config.columnModifier),
        horizontalAlignment = config.columnHorizontalAlignment,
        verticalArrangement = config.columnVerticalArrangement,
    ) {
        FloatingActionButton(
            onClick = onCancel,
            shape = config.buttonsShape,
            containerColor = config.cancelButtonColor,
            modifier = config.buttonsModifier,
        ) {
            Icon(
                imageVector = if (canUndo) config.cancelButtonImageWithValues else config.cancelButtonImageWithoutValues,
                contentDescription = "Cancel route",
                tint = config.cancelButtonTint,
            )
        }

        FloatingActionButton(
            onClick = onUndo,
            shape = config.buttonsShape,
            containerColor = if (canUndo) config.undoButtonEnableColor else config.undoButtonDisableColor,
            modifier = config.buttonsModifier,
        ) {
            Icon(
                imageVector = config.undoButtonImage,
                contentDescription = "Undo last point",
                tint = config.undoButtonTint,
            )
        }

        FloatingActionButton(
            onClick = onComplete,
            shape = config.buttonsShape,
            containerColor = if (canComplete) config.submitButtonEnableColor else config.submitButtonDisableColor,
            modifier = config.buttonsModifier,
        ) {
            Icon(
                imageVector = config.submitButtonImage,
                contentDescription = "Complete route",
                tint = config.submitButtonTint,
            )
        }
    }
}

@Composable
fun BoxScope.TrailsButton(
    config: DrawingButtonsConfig,
    onTrailsClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onTrailsClick,
        modifier =
            Modifier
                .align(Alignment.BottomStart)
                .then(config.columnModifier.padding(start = 48.dp + 8.dp)) // size + spaceBetween
                .then(config.buttonsModifier),
        shape = config.buttonsShape,
        containerColor = config.createButtonColor,
        content = {
            Icon(
                imageVector = Icons.Filled.Route,
                contentDescription = "Start route creation",
                tint = Color.Black,
            )
        },
    )
}
