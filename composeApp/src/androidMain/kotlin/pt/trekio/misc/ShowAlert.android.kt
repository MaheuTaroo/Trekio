package pt.trekio.misc

import android.widget.Toast

actual fun showAlert(msg: String) {
    Toast.makeText(AppContext.get(), msg, Toast.LENGTH_SHORT).show()
}
