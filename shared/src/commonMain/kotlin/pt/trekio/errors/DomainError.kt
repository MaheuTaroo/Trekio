package pt.trekio.errors

import pt.trekio.dto.ErrorMessage

sealed class DomainError(
    val statusCode: Int,
    open val message: String,
) {
    private constructor(error: String) : this(400, error)

    data object NegativeSkip : DomainError("Skip value must be positive or zero")

    data object NonPositiveLimit : DomainError("Limit value must be positive")

    data object UnexpectedError : DomainError(500, "Unexpected error")

    data class MissingParameter(
        private val paramName: String,
    ) : DomainError("Missing vital parameter: $paramName")

    data class MalformedParameter(
        private val expectedType: String,
    ) : DomainError("Malformed parameter type; expected $expectedType")

    data class IncorrectMediaType(
        private val types: List<String>,
    ) : DomainError(
            415,
            "Incorrect media type; supported media types: ${types.joinToString(", ")}",
        )
}

fun DomainError.toErrorMessage() = ErrorMessage(message)
