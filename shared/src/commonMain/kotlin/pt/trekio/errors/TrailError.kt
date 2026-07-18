package pt.trekio.errors

sealed class TrailError private constructor(
    statusCode: Int,
    message: String,
) : DomainError(statusCode, message) {
    private constructor(message: String) : this(400, message)

    data object TrailNotFound : TrailError(404, "Trail not found")

    data object ParentTrailNotFound : TrailError(404, "Parent trail not found")

    data object TrailCannotParentItself : TrailError("Trail's parent cannot be itself")

    data object WrongTrailFormat : TrailError("Trail data not conforming to KML format")

    data class TrailNotOwnedByUser(
        private val wasEditing: Boolean,
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
        private val name: String,
    ) : TrailError("Invalid trail name \"$name\"")

    data object TrailTooShort : TrailError("Trail must have at least one point between start and finish")

    data object UserIsNotVerified : TrailError("You must be verified to create a trail")
}
