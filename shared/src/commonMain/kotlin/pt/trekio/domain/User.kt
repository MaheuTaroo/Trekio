package pt.trekio.domain

import pt.trekio.dto.UserDto
import pt.trekio.misc.Email
import pt.trekio.misc.UserRank
import pt.trekio.misc.Username

data class User(
    val id: ULong,
    val username: Username,
    val email: Email,
    val passwordValidInfo: String?,
    val rank: UserRank = UserRank.NEW,
)

fun User.toDto() = UserDto(username.value, rank.name)
