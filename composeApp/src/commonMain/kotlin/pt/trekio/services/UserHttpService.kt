package pt.trekio.services

import pt.trekio.misc.Either
import pt.trekio.misc.TokenExternalInfo

class UserHttpService : UserService {
    override fun signUp(
        username: String,
        email: String,
        password: String,
    ): Either<String, TokenExternalInfo> {
        TODO("Not yet implemented")
    }

    override fun login(
        email: String,
        password: String,
    ): Either<String, TokenExternalInfo> {
        TODO("Not yet implemented")
    }

    override fun delete(): Either<String, Unit> {
        TODO("Not yet implemented")
    }
}
