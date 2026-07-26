package pt.trekio.services

import pt.trekio.dto.StatisticsDto
import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.dto.UserDto
import pt.trekio.misc.Either
import pt.trekio.misc.success
import pt.trekio.services.user.UserService
import pt.trekio.services.utils.TestUser.ACCESS_TOKEN
import pt.trekio.services.utils.TestUser.EXPIRATION
import pt.trekio.services.utils.TestUser.GOOGLE_SUCCESS
import pt.trekio.services.utils.TestUser.RANK
import pt.trekio.services.utils.TestUser.REFRESH_TOKEN
import pt.trekio.services.utils.TestUser.TOTAL_KMS
import pt.trekio.services.utils.TestUser.TOTAL_TIME
import pt.trekio.services.utils.TestUser.TRAILS
import pt.trekio.services.utils.TestUser.UID
import pt.trekio.services.utils.TestUser.USERNAME

object SuccessfulUserService : UserService {
    override suspend fun signUp(
        username: String,
        email: String,
        password: String,
    ): Either<String, TokenExternalInfoDto> =
        success(
            TokenExternalInfoDto(
                accessTokenValue = ACCESS_TOKEN,
                refreshTokenValue = REFRESH_TOKEN,
                tokenExpiration = EXPIRATION,
            ),
        )

    override suspend fun login(
        email: String,
        password: String,
    ): Either<String, TokenExternalInfoDto> =
        success(
            TokenExternalInfoDto(
                accessTokenValue = ACCESS_TOKEN,
                refreshTokenValue = REFRESH_TOKEN,
                tokenExpiration = EXPIRATION,
            ),
        )

    override suspend fun logout(): Either<String, Unit> = success(Unit)

    override suspend fun getSelfDetails(): Either<String, UserDto> =
        success(
            UserDto(
                id = UID,
                username = USERNAME,
                rank = RANK,
            ),
        )

    override suspend fun getUserByIdentifier(identifier: String): Either<String, UserDto> =
        success(
            UserDto(
                id = UID,
                username = USERNAME,
                rank = RANK,
            ),
        )

    override suspend fun getStatsOf(id: ULong): Either<String, StatisticsDto> =
        success(
            StatisticsDto(
                uid = UID,
                trails = TRAILS,
                totalKms = TOTAL_KMS,
                totalTime = TOTAL_TIME,
            ),
        )

    override suspend fun updateDetails(
        username: String?,
        password: String?,
    ): Either<String, TokenExternalInfoDto> =
        success(
            TokenExternalInfoDto(
                accessTokenValue = ACCESS_TOKEN,
                refreshTokenValue = REFRESH_TOKEN,
                tokenExpiration = EXPIRATION,
            ),
        )

    override suspend fun deleteUser(): Either<String, Unit> = success(Unit)

    override suspend fun googlePopup(): Either<String, String> = success(GOOGLE_SUCCESS)

    override suspend fun googleCallback(
        code: String,
        email: String,
        username: String,
    ): Either<String, TokenExternalInfoDto> =
        success(
            TokenExternalInfoDto(
                accessTokenValue = ACCESS_TOKEN,
                refreshTokenValue = REFRESH_TOKEN,
                tokenExpiration = EXPIRATION,
            ),
        )
}
