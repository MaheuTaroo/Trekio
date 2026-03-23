package pt.trekio.errors

sealed class UserError(
    val error: String,
) {
    data object UsernameAlreadyExists : UserError("Username already exists")

    data object UserDoesNotExist : UserError("User does not exist")

    data object NegativeSkip : UserError("Skip value must be positive or zero")

    data object NonPositiveLimit : UserError("Limit value must be positive")

    data object TokenDoesNotExist : UserError("Token does not exist")

    data object MissingToken : UserError("Cannot authorize, token is missing")

    data object InvalidToken : UserError("Cannot authorize, token is malformed")

    data object ExpiredToken : UserError("Token has already expired")

    data object UnexpectedError : UserError("Unexpected error")
}
