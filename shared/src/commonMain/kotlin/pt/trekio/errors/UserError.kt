package pt.trekio.errors

sealed class UserError private constructor(
    statusCode: Int,
    error: String,
) : DomainError(statusCode, error) {
    data object UsernameAlreadyExists : UserError(400, "Username already exists")

    data object EmailAlreadyUsed : UserError(400, "Email already in use")

    data object UserDoesNotExist : UserError(404, "User does not exist")

    data object InvalidToken : UserError(401, "Cannot authorize, token is malformed")

    data object ExpiredToken : UserError(403, "Token has already expired")

    data class InvalidUsername(
        override val message: String,
    ) : UserError(400, message)

    data class InvalidEmail(
        override val message: String,
    ) : UserError(400, message)

    data class InvalidPassword(
        override val message: String,
    ) : UserError(400, message)

    data object IncorrectPassword : UserError(403, "Incorrect password")

    data object OAuthFailure : UserError(401, "OAuth failure")

    data object OAuthGetInfoFailure : UserError(401, "Couldn't retrieve user email from Google")

    data object EmailUsedInOAuth : UserError(
        400,
        "Email used in OAuth, update account with password for this feature",
    )
}
