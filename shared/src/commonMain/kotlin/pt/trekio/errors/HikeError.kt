package pt.trekio.errors

sealed class HikeError private constructor(
    statusCode: Int,
    message: String,
) : DomainError(statusCode, message) {
    data object HikeNotFound : HikeError(404, "Hike not found")

    data object NotOnTheHike : HikeError(403, "You are not on this hike")

    data object CurrentlyHiking : HikeError(400, "You are in the middle of a hike")

    data object NotCurrentlyHiking : HikeError(400, "You are not on a hike right now")

    data object InvalidStartingPoint : HikeError(
        400,
        "You're not on either of this trail's extremities",
    )

    data object InvalidEndingPoint : HikeError(400, "You haven't crossed the finish line yet!")

    class CouldNotStartHike(cause: String) : HikeError(500, "Could not start hiking: $cause")

    data object IncorrectWebSocketFormat : HikeError(
        400,
        "Incorrect location data format, must be \"(<latitude>;<longitude>;<altitude>)\""
    )
}
