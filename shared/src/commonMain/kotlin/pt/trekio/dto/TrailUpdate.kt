package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class TrailUpdate(
    val name: String,
    val parent: ULong?,
)
