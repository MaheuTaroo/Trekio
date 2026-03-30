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
        val POSSIBLE_CHARACTERS = (LETTERS union '0'..'9') + '_' + '.'
    }

    init {
        require(value.length in 3..32) { "Username must be between 3 and 32 characters long" }
        require(
            value.all {
                it in POSSIBLE_CHARACTERS
            },
        ) { "Username can only have uppercase and lowercase letters, digits, periods and underscores" }
        require(value[0] in LETTERS) { "Username must start with a letter" }
    }
}
