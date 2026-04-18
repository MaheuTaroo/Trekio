package pt.trekio.errors

sealed class UserError(
    statusCode: Int,
    error: String,
) : DomainError(statusCode, error) {
    data object UsernameAlreadyExists : UserError(409, "Username already exists")

    data object EmailAlreadyUsed : UserError(409, "Email already in use")

    data object UserDoesNotExist : UserError(404, "User does not exist")

    data object TokenDoesNotExist : UserError(401, "Token does not exist")

    data object InvalidToken : UserError(401, "Cannot authorize, token is malformed")

    data object ExpiredToken : UserError(403, "Token has already expired")

    data class InvalidUsername(
        override val error: String,
    ) : UserError(400, error)

    data class InvalidEmail(
        override val error: String,
    ) : UserError(400, error)

    data class InvalidPassword(
        override val error: String,
    ) : UserError(400, error)

    data object IncorrectPassword : UserError(403, "Incorrect password")
}
