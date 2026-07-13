package pt.trekio.ui.theme

import androidx.compose.ui.graphics.Color

object JournalPalette {
    val background = Color(0xFFF7F4ED) // warm paper
    val surface = Color(0xFFFFFFFF) // cards
    val surfaceVariant = Color(0xFFEFEBE1) // subtle inset surfaces (e.g. stat chips)
    val onBackground = Color(0xFF2B2A26) // near-black ink, not pure black
    val onSurfaceMuted = Color(0xFF6B6860) // secondary text, captions
    val outline = Color(0xFFDDD8CC) // hairline borders

    val accent = Color(0xFF4C7184) // slate blue — stats, active, primary actions
    val onAccent = Color(0xFFFFFFFF)
    val accentMuted = Color(0xFFE1E9EC) // accent tint for chip backgrounds

    val danger = Color(0xFFCE4D32) // delete / destructive actions
    val warning = Color(0xFFA86A1D)
}

// ---------- Topo (dark) ----------
object TopoPalette {
    val background = Color(0xFF1A1A18) // near-black, warm not blue-black
    val surface = Color(0xFF232320) // cards sit one step up from bg
    val surfaceVariant = Color(0xFF2C2C28) // inset surfaces
    val onBackground = Color(0xFFEDEAE2) // warm off-white, not pure white
    val onSurfaceMuted = Color(0xFF9C998E) // secondary text, captions
    val outline = Color(0xFF42423B) // hairline borders

    val accent = Color(0xFF2F8270) // teal-ink — stats, active, primary actions
    val onAccent = Color(0xFFEDEAE2)
    val accentMuted = Color(0xFF1E332D) // accent tint for chip backgrounds

    val danger = Color(0xFFCE4D32)
    val warning = Color(0xFFE0B655)
}
