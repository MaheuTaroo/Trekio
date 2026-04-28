package pt.trekio.services.user

import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.misc.Either
import pt.trekio.misc.TokenExternalInfo

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

    suspend fun getDetails(

    )

    suspend fun delete(): Either<String, Unit>
}
