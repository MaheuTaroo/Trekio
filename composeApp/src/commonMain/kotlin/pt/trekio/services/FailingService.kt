package pt.trekio.services

import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.misc.Either
import pt.trekio.misc.failure
import pt.trekio.services.user.UserService

object FailingService : UserService {
    private const val ERROR = "You are not logged in"

    override suspend fun signUp(
        username: String,
        email: String,
        password: String,
    ): Either<String, TokenExternalInfoDto> = failure(ERROR)

    override suspend fun login(
        email: String,
        password: String,
    ): Either<String, TokenExternalInfoDto> = failure(ERROR)

    override suspend fun getDetails() {
        TODO("Not yet implemented")
    }

    override suspend fun delete(): Either<String, Unit> = failure(ERROR)

    override suspend fun googlePopup(): Either<String, String> = failure(ERROR)

    override suspend fun googleCallback() {

    }
}
