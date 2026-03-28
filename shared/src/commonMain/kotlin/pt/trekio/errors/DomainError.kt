package pt.trekio.errors

import pt.trekio.dto.ErrorMessage

abstract class DomainError(val error: String) {
    fun toErrorMessage() = ErrorMessage(error)
}