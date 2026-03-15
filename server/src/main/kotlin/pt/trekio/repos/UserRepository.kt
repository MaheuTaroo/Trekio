package pt.trekio.repos

import pt.trekio.dto.User
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Token
import kotlin.time.Instant

interface UserRepository {

    /**
     * Creates a new user based on the provided information.
     * @param user The new user's information.
     * @return Either the cause of failure, or nothing in case of success.
     */
    fun createUser(user: User): Either<UserError, Unit>

    /**
     * Retrieves the user's information based on their username.
     * @param username The user's name.
     * @return The user's information, or ``null`` if none exists,
     */
    fun getUser(username: String): User?

    /**
     * Retrieves a paginated list of users.
     * @param skip The amount of users to skip.
     * @param limit The amount of users to include.
     * @return A paginated list of all existing users.
     */
    fun getUsers(skip: Int, limit: Int): List<User>

    /**
     * Updates a user's information.
     * @param username The user's name.
     * @param updatedInfo The user's new information.
     * @return The cause of failure, or nothing in case of success.
     */
    fun updateUser(username: String, updatedInfo: User): Either<UserError, Unit>

    /**
     * Deletes a user's information.
     * @param username The name of the user to delete.
     * @return The cause of failure, or nothing in case of success.
     */
    fun deleteUser(username: String): Either<UserError, Unit>

    /**
     * Clears the user repo.
     */
    fun deleteAllUsers()

    /**
     * Retrieves the user and their token based on its validation info.
     * @param tokenValidationInfo The token's validation info.
     * @return A user-token pair, or null if the validation info isn't bound to a token.
     */
    fun getTokenByTokenValidationInfo(tokenValidationInfo: String): Pair<User, Token>?

    /**
     * Creates a new token.
     * @param token The token to create.
     * @param maxTokens The maximum amount of allowed tokens on storage.
     * @return The cause of failure, or nothing in case of success.
     */
    fun createToken(
        token: Token,
        maxTokens: Int,
    ): Either<UserError, Unit>

    /**
     * Updates the token's last usage timestamp.
     * @param token The token to update.
     * @param now The last usage timestamp.
     * @return The cause of failure, or nothing in case of success.
     */
    fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ): Either<UserError, Unit>

    /**
     * Removes one or more tokens from storage by their validation info.
     * @param tokenValidationInfo The token's validation info.
     * @return The amount of removed tokens.
     */
    fun removeTokenByValidationInfo(tokenValidationInfo: String): Int
}