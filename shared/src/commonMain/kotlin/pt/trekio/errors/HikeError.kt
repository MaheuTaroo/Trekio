package pt.trekio.errors

sealed class HikeError private constructor(
    statusCode: Int,
    message: String,
) : DomainError(statusCode, message) {
    private constructor(message: String) : this(400, message)

    data object HikeNotFound : HikeError(404, "Hike not found")

    data object NotOnTheHike : HikeError(403, "You are not on this hike")

    data object CurrentlyHiking : HikeError("You are in the middle of a hike")

    data object NotCurrentlyHiking : HikeError("You are not on a hike right now")

    data object InvalidStartingPoint : HikeError("You're not on either of this trail's extremities")

    data object InvalidEndingPoint : HikeError("You haven't crossed the finish line yet!")

    data class CouldNotStartHike(
        private val cause: String,
    ) : HikeError(500, "Could not start hiking: $cause")

    data object IncorrectWebSocketFormat : HikeError(
        "Incorrect location data format, must be \"(<latitude>;<longitude>;<altitude>)\"",
    )
}
