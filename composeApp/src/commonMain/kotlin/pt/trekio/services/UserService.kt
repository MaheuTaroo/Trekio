package pt.trekio.services

import pt.trekio.misc.Either
import pt.trekio.misc.TokenExternalInfo

interface UserService {
    fun signUp(
        username: String,
        email: String,
        password: String,
    ): Either<String, TokenExternalInfo>

    fun login(
        email: String,
        password: String,
    ): Either<String, TokenExternalInfo>

    fun delete(): Either<String, Unit>
}
