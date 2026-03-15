package pt.trekio.repos

import pt.trekio.dto.Token
import pt.trekio.dto.User
import kotlin.time.Instant

interface UserDataAccess {
    fun createUser(user: User)

    fun getUser(username: String): User?

    fun getUsers(limit: Int, skip: Int): List<User>

    fun updateUser(user: User)

    fun deleteUser(username: String)

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
     */
    fun createToken(
        token: Token,
        maxTokens: Int,
    )

    /**
     * Updates the token's last usage timestamp.
     * @param token The token to update.
     * @param now The last usage timestamp.
     */
    fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    )

    /**
     * Removes one or more tokens from storage by their validation info.
     * @param tokenValidationInfo The token's validation info.
     * @return The amount of removed tokens.
     */
    fun removeTokenByValidationInfo(tokenValidationInfo: String): Int
}