package pt.trekio.dto

import kotlinx.serialization.Serializable

@Serializable
data class TrailCreate(
    val name: String,
    val start: GeoPointDto,
    val end: GeoPointDto,
    val path: List<GeoPointDto>,
    val parentId: ULong?,
)
