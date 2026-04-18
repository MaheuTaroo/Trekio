package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class TrailPointDto(
    val lat: Double,
    val lon: Double,
    val alt: Double,
)
