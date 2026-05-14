package pt.trekio.services.user

import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.misc.Either

interface UserService {
    suspend fun signUp(
        username: String,
        email: String,
        password: String,
    ): Either<String, TokenExternalInfoDto>

    suspend fun login(
        email: String,
        password: String,
    ): Either<String, TokenExternalInfoDto>

    suspend fun getDetails()

    suspend fun delete(): Either<String, Unit>

    suspend fun googlePopup(): Either<String, String>

    suspend fun googleCallback()
}
