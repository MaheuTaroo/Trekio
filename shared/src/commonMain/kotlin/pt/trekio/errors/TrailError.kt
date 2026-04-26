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
    ) : TrailError(
            415,
            "Expected application/xml, text/xml or application/vnd.google-earth.kml+xml; got $actualFormat",
        )

    data object WrongTrailFormat : TrailError(400, "Trail data not conforming to KML format")

    data class TrailNotOwnedByUser(
        val wasEditing: Boolean,
    ) : TrailError(
            403,
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

    data object TrailTooShort : TrailError(
        400,
        "Trail must have at least one point between start and finish",
    )
}
