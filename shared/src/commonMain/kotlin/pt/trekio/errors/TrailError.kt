package pt.trekio.errors

sealed class TrailError(
    statusCode: Int,
    message: String,
) : DomainError(statusCode, message) {
    data object TrailNotFound : TrailError(404, "Trail not found")

    data object ParentTrailNotFound : TrailError(404, "Parent trail not found")

    data object TrailCannotParentItself : TrailError(400, "Trail's parent cannot be itself")

    data class KMLExpected(
        val actualFormat: String,
    ) : TrailError(400, "Expected application/vnd.google-earth.kml+xml, got $actualFormat")

    data class TrailNotOwnedByUser(
        val wasEditing: Boolean,
    ) : TrailError(
            401,
            "This trail is not yours to " +
                if (wasEditing) {
                    "edit"
                } else {
                    "remove"
                },
        )

    data class InvalidTrailName(
        override val error: String,
    ) : TrailError(400, error)
}
