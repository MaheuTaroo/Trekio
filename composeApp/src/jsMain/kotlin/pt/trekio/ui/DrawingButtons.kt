package pt.trekio.ui

import io.github.tiagopraia.kmp.mapbox.BUTTON_ICON_SIZE
import io.github.tiagopraia.kmp.mapbox.BUTTON_MARGIN
import io.github.tiagopraia.kmp.mapbox.BUTTON_RADIUS
import io.github.tiagopraia.kmp.mapbox.BUTTON_SHADOW
import io.github.tiagopraia.kmp.mapbox.BUTTON_SIZE
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLLinkElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.events.Event

private const val BUTTON_GAP = "8px"
private const val COLOR_WHITE = "#FFFFFF"
private const val COLOR_GRAY = "#BDBDBD"
private const val COLOR_RED = "#F44336"
private const val COLOR_BLUE = "#29B6F6"
private const val COLOR_ICON_DARK = "#333333"
private const val COLOR_ICON_LIGHT = "#FFFFFF"

fun buildOverlayButtons(
    onProfileClick: () -> Unit,
    onTrailsClick: () -> Unit,
    isDrawingMode: Boolean,
    canUndo: Boolean,
    canComplete: Boolean,
    onStartRoute: () -> Unit,
    onUndo: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
): List<HTMLElement> =
    buildList {
        add(buildProfileButton(onProfileClick))
        add(
            buildDrawingColumn(
                isDrawingMode = isDrawingMode,
                canUndo = canUndo,
                canComplete = canComplete,
                onStartRoute = onStartRoute,
                onUndo = onUndo,
                onComplete = onComplete,
                onCancel = onCancel,
            ),
        )
        add(buildTrailsButton(onTrailsClick))
    }

private fun buildProfileButton(onClick: () -> Unit): HTMLButtonElement =
    buildFab(COLOR_WHITE, "person", onClick).apply {
        style.position = "absolute"
        style.top = BUTTON_MARGIN
        style.right = BUTTON_MARGIN
        style.asDynamic().pointerEvents = "auto"
    }

private fun buildTrailsButton(onClick: () -> Unit): HTMLButtonElement =
    buildFab(COLOR_WHITE, "route", onClick).apply {
        style.position = "absolute"
        style.bottom = BUTTON_MARGIN
        style.left = "calc($BUTTON_MARGIN + $BUTTON_SIZE + $BUTTON_GAP)"
        style.asDynamic().pointerEvents = "auto"
    }

private fun buildDrawingColumn(
    isDrawingMode: Boolean,
    canUndo: Boolean,
    canComplete: Boolean,
    onStartRoute: () -> Unit,
    onUndo: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
): HTMLDivElement {
    val column = document.createElement("div") as HTMLDivElement
    column.style.position = "absolute"
    column.style.bottom = BUTTON_MARGIN
    column.style.left = BUTTON_MARGIN
    column.style.display = "flex"
    column.style.asDynamic().flexDirection = "column"
    column.style.asDynamic().gap = BUTTON_GAP
    column.style.asDynamic().pointerEvents = "auto"

    if (isDrawingMode) {
        column.appendChild(buildCancelButton(onCancel, canUndo))
        column.appendChild(buildUndoButton(onUndo, canUndo))
        column.appendChild(buildCompleteButton(onComplete, canComplete))
    } else {
        column.appendChild(buildCreateButton(onStartRoute))
    }

    return column
}

private fun buildCreateButton(onClick: () -> Unit): HTMLButtonElement = buildFab(COLOR_WHITE, "add", onClick)

private fun buildCancelButton(
    onClick: () -> Unit,
    canUndo: Boolean,
): HTMLButtonElement = buildFab(COLOR_RED, if (canUndo) "delete" else "close", onClick)

private fun buildUndoButton(
    onClick: () -> Unit,
    canUndo: Boolean,
): HTMLButtonElement = buildFab(if (canUndo) COLOR_WHITE else COLOR_GRAY, "undo", onClick)

private fun buildCompleteButton(
    onClick: () -> Unit,
    canComplete: Boolean,
): HTMLButtonElement = buildFab(if (canComplete) COLOR_BLUE else COLOR_GRAY, "check", onClick)

private fun buildFab(
    backgroundColor: String,
    icon: String,
    onClick: () -> Unit,
): HTMLButtonElement {
    injectMaterialIcons()

    val button = document.createElement("button") as HTMLButtonElement
    button.style.width = BUTTON_SIZE
    button.style.height = BUTTON_SIZE
    button.style.borderRadius = BUTTON_RADIUS
    button.style.backgroundColor = backgroundColor
    button.style.border = "none"
    button.style.cursor = "pointer"
    button.style.display = "flex"
    button.style.asDynamic().alignItems = "center"
    button.style.asDynamic().justifyContent = "center"
    button.style.asDynamic().boxShadow = BUTTON_SHADOW
    button.style.asDynamic().flexShrink = "0"

    val span = document.createElement("span") as HTMLSpanElement
    span.className = "material-icons"
    span.textContent = icon
    span.style.fontSize = BUTTON_ICON_SIZE
    span.style.color =
        if (backgroundColor == COLOR_WHITE || backgroundColor == COLOR_GRAY) {
            COLOR_ICON_DARK
        } else {
            COLOR_ICON_LIGHT
        }
    button.appendChild(span)

    button.addEventListener("click", { _: Event -> onClick() })
    return button
}

private fun injectMaterialIcons() {
    if (document.getElementById("material-icons-css") != null) return
    val link = document.createElement("link") as HTMLLinkElement
    link.id = "material-icons-css"
    link.rel = "stylesheet"
    link.href = "https://fonts.googleapis.com/icon?family=Material+Icons"
    document.head?.appendChild(link)
}
