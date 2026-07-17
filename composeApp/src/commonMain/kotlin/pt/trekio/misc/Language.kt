package pt.trekio.misc

enum class Language(
    val tag: String,
    val displayName: String,
    val flag: String,
) {
    English("en", "English", "\uD83C\uDDEC\uD83C\uDDE7"),
    German("de-DE", "Deutsch", "\uD83C\uDDE9\uD83C\uDDEA"),
    Spanish("es", "Español", "\uD83C\uDDEA\uD83C\uDDF8"),
    French("fr-FR", "Français", "\uD83C\uDDEB\uD83C\uDDF7"),
    Portuguese("pt-PT", "Português", "\uD83C\uDDF5\uD83C\uDDF9"),
    ;

    companion object {
        fun fromTag(tag: String): Language = entries.firstOrNull { it.tag == tag } ?: English
    }
}
