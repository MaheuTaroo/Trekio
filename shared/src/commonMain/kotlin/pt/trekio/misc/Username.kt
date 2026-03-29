package pt.trekio.misc

import kotlin.jvm.JvmInline

/**
 * Represents the name of a user, which must follow these rules:
 * - a minimum size of 3 characters
 * - start with a letter
 * @property value The user's name.
 */
@JvmInline
value class Username(
    val value: String,
) {
    private companion object {
        val LETTERS = 'a'..'z' union 'A'..'Z'
    }

    init {
        require(value.length > 2) { "Username must be at least 3 characters long" }
        require(value[0] in LETTERS) { "Username must start with a letter" }
    }
}
