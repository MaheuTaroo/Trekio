package pt.trekio.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.tiagopraia.kmp.mapbox.configs.FollowButtonConfig

@Composable
fun BoxScope.MapOverlayButtons(
    followButtonConfig: FollowButtonConfig,
    isDrawingMode: Boolean,
    canUndo: Boolean,
    canComplete: Boolean,
    onProfileClick: () -> Unit,
    onTrailsClick: () -> Unit,
    onStartRoute: () -> Unit,
    onUndo: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
) {
    val drawingConfig = remember { DrawingButtonsConfig() }

    ProfileButton(followButtonConfig, onProfileClick)
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
    config: FollowButtonConfig,
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
