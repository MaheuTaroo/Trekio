package pt.trekio.services.user

import pt.trekio.dto.StatisticsDto
import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.dto.UserDto
import pt.trekio.misc.Either

interface UserService {
    /**
     * Registers a new user.
     * @param username The user's name.
     * @param email The user's email.
     * @param password The user's password.
     * @return the new user's tokens in case of success, or
     * an error message in case of failure.
     */
    suspend fun signUp(
        username: String,
        email: String,
        password: String,
    ): Either<String, TokenExternalInfoDto>

    /**
     * Logs a user in.
     * @param email The user's email.
     * @param password The user's password.
     * @return the user's tokens in case of success, or an
     * error message in case of failure.
     */
    suspend fun login(
        email: String,
        password: String,
    ): Either<String, TokenExternalInfoDto>

    /**
     * Logs a user out.
     * @return nothing in case of success, or an error
     * message in case of failure.
     */
    suspend fun logout(): Either<String, Unit>

    /**
     * Fetches the current user's details.
     * @return the user's information in case of success,
     * or an error message in case of failure.
     */
    suspend fun getOwnDetails(): Either<String, UserDto>

    /**
     * Fetches a user's hiking statistics.
     * @param id The user's identifier.
     * @return the user's statistics in case of success, or
     * an error message in case of failure.
     */
    suspend fun getStatsOf(id: ULong): Either<String, StatisticsDto>

    /**
     * Removes the current user.
     * @return nothing in case of success, or an error
     * message in case of failure.
     */
    suspend fun deleteUser(): Either<String, Unit>

    suspend fun googlePopup(): Either<String, String>

    suspend fun googleCallback()
}
