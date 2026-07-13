package pt.trekio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.tiagopraia.kmp.mapbox.config.AndroidFollowButtonConfig
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
                    label = "Profile",
                    angle = 270f,
                    onClick = { onProfileClick() },
                ),
                Option(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = "Logout",
                    angle = 30f,
                    onClick = { onLogout() },
                ),
                Option(
                    icon = Icons.Default.Settings,
                    label = "Settings",
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
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = onRouteNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Nome") },
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
                        Text("Cancel")
                    }

                    Button(
                        onClick = onCommit,
                        enabled = !isCommitLoading && routeName.isNotBlank(),
                    ) {
                        Text("Commit")
                    }
                }
            }
        }
    }
}

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
fun BoxScope.ProfileButton(
    config: AndroidFollowButtonConfig,
    onProfileClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onProfileClick,
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .then(Modifier.padding(top = 20.dp))
                .then(config.buttonModifier),
        shape = config.buttonShape,
        containerColor = config.followButtonColor,
        content = {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Start route creation",
                tint = Color.Black,
            )
        },
    )
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
