package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserCredentialLogin(
    val email: String,
    val password: String
)
