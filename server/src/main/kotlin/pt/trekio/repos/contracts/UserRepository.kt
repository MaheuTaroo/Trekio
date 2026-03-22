package pt.trekio.repos.contracts

import pt.trekio.domain.User
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Token
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

abstract class UserRepository {
    protected val tokenLifetime = 24.hours

    /**
     * Creates a new user based on the provided information.
     * @param name The new user's name.
     * @param email The user's email.
     * @param passHash The user's hashed password.
     * @return Either the cause of failure, or nothing in case of success.
     */
    abstract fun createUser(
        name: String,
        email: String,
        passHash: String,
    ): Either<UserError, User>

    /**
     * Retrieves the user's information based on their username.
     * @param username The user's name.
     * @return The user's information, or ``null`` if none exists,
     */
    abstract fun getUser(username: String): User?

    /**
     * Retrieves a paginated list of users.
     * @param skip The amount of users to skip.
     * @param limit The amount of users to include.
     * @return A paginated list of all existing users.
     */
    abstract fun getUsers(
        skip: Int,
        limit: Int,
    ): List<User>

    /**
     * Updates a user's information.
     * @param name The user's name.
     * @param updatedInfo The user's new information.
     * @return The cause of failure, or nothing in case of success.
     */
    abstract fun updateUser(
        name: String,
        updatedInfo: User,
    ): Either<UserError, Unit>

    /**
     * Deletes a user's information.
     * @param username The name of the user to delete.
     * @return The cause of failure, or nothing in case of success.
     */
    abstract fun deleteUser(username: String): Either<UserError, Unit>

    /**
     * Clears the user repo.
     */
    abstract fun deleteAllUsers()

    /**
     * Retrieves the user and their token based on its validation info.
     * @param tokenValidationInfo The token's validation info.
     * @return A user-token pair, or null if the validation info isn't bound to a token.
     */
    abstract fun getTokenByTokenValidationInfo(tokenValidationInfo: String): Pair<User, Token>?

    /**
     * Creates a new token.
     * @param token The token to create.
     * @param maxTokens The maximum amount of allowed tokens on storage.
     * @return The cause of failure, or nothing in case of success.
     */
    abstract fun createToken(
        token: Token,
        maxTokens: Int,
    ): Either<UserError, Unit>

    /**
     * Updates the token's last usage timestamp.
     * @param token The token to update.
     * @param now The last usage timestamp.
     * @return The cause of failure, or nothing in case of success.
     */
    abstract fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ): Either<UserError, Unit>

    /**
     * Removes one or more tokens from storage by their validation info.
     * @param tokenValidationInfo The token's validation info.
     * @return The amount of removed tokens.
     */
    abstract fun removeTokenByValidationInfo(tokenValidationInfo: String): Int
}
