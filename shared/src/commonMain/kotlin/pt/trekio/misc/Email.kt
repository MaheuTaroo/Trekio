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
    private companion object {
        val ALPHANUMERIC = ('a'..'z' union 'A'..'Z' union '0'..'9')

        // Effectively evaluates if there are 2 consecutive non-alphanumeric characters
        val CONSECUTIVE_SPECIAL_CHARS = Regex("[^a-zA-Z0-9]{2,}")
    }

    init {
        require(value.none(Char::isWhitespace)) { "Email should not contain whitespaces" }
        require(CONSECUTIVE_SPECIAL_CHARS !in value) { "Email should not contain 2 consecutive non-alphanumeric characters" }

        val parts = value.split("@")

        require(parts.size == 2) { "Email should contain one and only one '@'" }

        val (local, domain) = parts

        require(local.isNotEmpty() && local[0] in ALPHANUMERIC) { "Email should start with a uppercase and lowercase letters or a digit" }

        require(domain.isNotEmpty() && domain.last() in ALPHANUMERIC) { "Email should not end with a special character" }

        val domainParts = domain.split('.')

        require(domainParts.size >= 2) { "Email should contain at least one period for top-level domain" }
    }
}
