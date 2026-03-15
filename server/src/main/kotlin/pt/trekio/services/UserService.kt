package pt.trekio.services

import pt.trekio.dto.User
import pt.trekio.dto.UserRank
import pt.trekio.errors.UserError
import pt.trekio.misc.Either
import pt.trekio.misc.Success
import pt.trekio.misc.failure
import pt.trekio.misc.success
import pt.trekio.repos.UserRepository

class UserService(private val repo: UserRepository) {
    private companion object {
        val possibleChars = 'a'..'z' union 'A'..'Z' union '0'..'9'

        fun createRandomUsername(): String {
            var name = "";

            repeat(32) {
                name += possibleChars.random()
            }

            return name
        }
    }

    fun createRandomUser(): Success<User> {
        var newName = createRandomUsername()
        while (repo.getUser(newName) != null)
            newName = createRandomUsername()

        val newUser =
            User(
                createRandomUsername(),
                null,
                "",
                UserRank.NEW,
                0,
                0.0,
                0
            )

        repo.createUser(newUser)
        return Success(newUser)
    }

    fun getUsers(skip: Int, limit: Int): Either<UserError, List<User>> {
        if (skip < 0) return failure(UserError.NegativeSkip)
        if (limit < 1) return failure(UserError.NonPositiveLimit)

        return success(repo.getUsers(skip, limit))
    }
}