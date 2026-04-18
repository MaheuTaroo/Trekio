package pt.trekio.errors

import pt.trekio.dto.ErrorMessage

sealed class DomainError(
    val statusCode: Int,
    open val error: String,
) {
    data object NegativeSkip : UserError(400, "Skip value must be positive or zero")

    data object NonPositiveLimit : UserError(400, "Limit value must be positive")

    data object UnexpectedError : UserError(500, "Unexpected error")

    data class MissingParameter(
        val paramName: String,
    ) : DomainError(400, "Missing vital parameter: $paramName")

    data class MalformedParameter(
        val expectedType: String,
    ) : DomainError(400, "Malformed parameter type; expected $expectedType")

    fun toErrorMessage() = ErrorMessage(error)
}
