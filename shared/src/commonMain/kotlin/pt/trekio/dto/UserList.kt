package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserList(
    val users: List<User>,
)
