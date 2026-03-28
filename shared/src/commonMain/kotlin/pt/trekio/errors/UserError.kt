package pt.trekio.errors

sealed class UserError(
    val statusCode: Int,
    error: String,
): DomainError(error) {
    data object UsernameAlreadyExists : UserError(409, "Username already exists")

    data object EmailAlreadyUsed : UserError(409, "Email already in use")

    data object UserDoesNotExist : UserError(404, "User does not exist")

    data object NegativeSkip : UserError(400, "Skip value must be positive or zero")

    data object NonPositiveLimit : UserError(400, "Limit value must be positive")

    data object TokenDoesNotExist : UserError(401, "Token does not exist")

    data object MissingToken : UserError(401, "Cannot authorize, token is missing")

    data object InvalidToken : UserError(401, "Cannot authorize, token is malformed")

    data object ExpiredToken : UserError(403, "Token has already expired")

    data object UnexpectedError : UserError(500, "Unexpected error")

    data object InvalidUsername : UserError(
        400,
        "Username must be at least 3 characters long and start with a letter",
    )

    data object InvalidEmail : UserError(400, "Invalid email")

    data object InvalidPassword : UserError(
        400,
        "Password must be at least 8 characters long and contain a combination of letters, numbers and symbols",
    )

    data object IncorrectPassword : UserError(400, "Incorrect password")
}