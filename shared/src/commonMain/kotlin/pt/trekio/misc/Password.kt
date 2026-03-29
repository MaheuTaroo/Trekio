package pt.trekio.misc

import kotlin.jvm.JvmInline

/**
 * Represents a plaintext password, which must follow these rules:
 * - a minimum size of 8 characters
 * - contain one lowercase letter
 * - contain one uppercase letter
 * - contain one digit
 * - contain one symbol
 * @property value The plaintext password.
 */
@JvmInline
value class Password(
    val value: String,
) {
    private companion object {
        val LOWERCASE = 'a'..'z'
        val UPPERCASE = 'A'..'Z'
        val DIGITS = '0'..'9'
        const val SYMBOLS = """!"#$%&/()=?»@£§€{[]}«+*-<>\|;:_"""
    }

    init {
        require(value.length > 7) { "Password must be at least 8 characters long" }
        require(value.any { it in LOWERCASE }) { "Password must contain at least one lowercase letter" }
        require(value.any { it in UPPERCASE }) { "Password must contain at least one uppercase letter" }
        require(value.any { it in DIGITS }) { "Password must contain at least one digit" }
        require(value.any { it in SYMBOLS }) { "Password must contain at least one symbol" }
    }
}

/**
 * Hashes this password.
 * @receiver The plaintext password.
 * @return The resulting hash.
 */
fun Password.hash() = value
