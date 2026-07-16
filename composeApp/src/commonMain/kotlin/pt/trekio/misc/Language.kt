package pt.trekio.misc

enum class Language(
    val tag: String,
    val displayName: String,
) {
    English("en", "English"),
    German("de-DE", "Deutsch"),
    Spanish("es", "Español"),
    French("fr-FR", "Français"),
    Portuguese("pt-PT", "Português"),
    ;

    companion object {
        fun fromTag(tag: String): Language = entries.firstOrNull { it.tag == tag } ?: English
    }
}
