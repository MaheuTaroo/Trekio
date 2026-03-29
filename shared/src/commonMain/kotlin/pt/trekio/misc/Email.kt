package pt.trekio.misc

import kotlin.jvm.JvmInline

/**
 * Represents a user's email, which follows these rules:
 * - a minimum size of 8 characters
 * - contain one lowercase letter
 * - contain one uppercase letter
 * - contain one digit
 * - contain one symbol
 * @property value The plaintext password.
 */
@JvmInline
value class Email(
    val value: String,
) {
    init {
        require(value.isNotEmpty()) { "Email should not be empty" }
        require(value.none { it == ' ' }) { "Email should not contain spaces" }
        require(value.length > 2) { "Email should contain at least 3 characters" }
        require(value.count { it == '@' } == 1) { "Email should contain one and only one '@'" }
        require(!value.endsWith('@')) { "Email should not end with an '@'" }
        require(!value.endsWith('.')) { "Email should not end with a period" }
        require(".." !in value) { "Email should not contain a period after another" }
    }
}
