package pt.trekio.errors

sealed class UserError private constructor(
    statusCode: Int,
    error: String,
) : DomainError(statusCode, error) {
    private constructor(error: String) : this(400, error)

    data object UsernameAlreadyExists : UserError("Username already exists")

    data object EmailAlreadyUsed : UserError("Email already in use")

    data object UserDoesNotExist : UserError(404, "User does not exist")

    data object InvalidToken : UserError(401, "Cannot authorize, token is malformed")

    data object ExpiredToken : UserError(403, "Token has already expired")

    data class InvalidUsername(
        override val message: String,
    ) : UserError(message)

    data class InvalidEmail(
        override val message: String,
    ) : UserError(message)

    data class InvalidPassword(
        override val message: String,
    ) : UserError(message)

    data object IncorrectPassword : UserError(403, "Incorrect password")

    data object OAuthFailure : UserError(401, "OAuth failure")

    data object OAuthGetInfoFailure : UserError(401, "Couldn't retrieve user email from Google")

    data object EmailUsedInOAuth : UserError(
        "Email used in OAuth, update account with password for this feature",
    )

    data object InvalidIdentifier : UserError("Identifier must be a positive numeric value or a valid username")
}
