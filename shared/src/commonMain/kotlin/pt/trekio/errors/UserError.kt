package pt.trekio.errors

sealed class UserError(val error: String) {
    data object UsernameAlreadyExists : UserError("Username already exists")

    data object UserDoesNotExist : UserError("User does not exist")

    data object NegativeSkip : UserError("Skip value must be positive or zero")

    data object NonPositiveLimit : UserError("Limit value must be positive")
}