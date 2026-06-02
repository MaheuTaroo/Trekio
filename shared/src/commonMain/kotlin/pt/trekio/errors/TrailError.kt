package pt.trekio.errors

sealed class TrailError private constructor(
    statusCode: Int,
    message: String,
) : DomainError(statusCode, message) {
    data object TrailNotFound : TrailError(404, "Trail not found")

    data object ParentTrailNotFound : TrailError(404, "Parent trail not found")

    data object TrailCannotParentItself : TrailError(400, "Trail's parent cannot be itself")

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
        override val message: String,
    ) : TrailError(400, message)

    data object TrailTooShort : TrailError(
        400,
        "Trail must have at least one point between start and finish",
    )

    data object UserIsNotVerified : TrailError(400, "You must be verified to create a trail")
}
