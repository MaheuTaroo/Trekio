package pt.trekio.misc

import kotlinx.browser.window

actual fun showAlert(msg: String) {
    window.alert(msg)
}
