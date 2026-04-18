package pt.trekio.repos.contracts

import pt.trekio.domain.User
import pt.trekio.errors.DomainError
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Email
import pt.trekio.misc.Password
import pt.trekio.misc.Token
import pt.trekio.misc.Username
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

abstract class UserRepository {
    protected val tokenLifetime = 24.hours

    /**
     * Creates a new user based on the provided information.
     * @param name The new user's name.
     * @param email The user's email.
     * @param password The user's hashed password.
     * @return Either the cause of failure, or nothing in case of success.
     */
    abstract fun createUser(
        name: Username,
        email: Email,
        password: Password,
    ): Either<DomainError, User>

    /**
     * Retrieves the user's information based on their internal ID.
     * @param id The user's internal ID.
     * @return The user's information, or ``null`` if none exists,
     */
    abstract fun getUserById(id: ULong): User?

    /**
     * Retrieves the user's information based on their username.
     * @param username The user's name.
     * @return The user's information, or ``null`` if none exists,
     */
    abstract fun getUserByName(username: Username): User?

    /**
     * Retrieves the user's information based on their email.
     * @param email The user's email.
     * @return The user's information, or ``null`` if none exists,
     */
    abstract fun getUserByEmail(email: Email): User?

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
        name: Username,
        updatedInfo: User,
    ): Either<UserError, Unit>

    /**
     * Deletes a user's information.
     * @param username The name of the user to delete.
     * @return The cause of failure, or nothing in case of success.
     */
    abstract fun deleteUser(username: Username): Either<UserError, Unit>

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
    ): Either<DomainError, Unit>

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
