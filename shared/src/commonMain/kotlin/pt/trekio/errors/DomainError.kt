package pt.trekio.errors

import pt.trekio.dto.ErrorMessage

abstract class DomainError(
    val statusCode: Int,
    open val error: String,
) {
    fun toErrorMessage() = ErrorMessage(error)
}
