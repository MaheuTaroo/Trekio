package pt.trekio.misc

object Routes {
    const val DOCS = "docs"

    const val USERS = "users"
    const val TRAILS = "trails"
    const val HIKES = "hikes"
    const val OAUTH = "oauth"
    const val GOOGLE = "google"

    const val CREATE = "create"
    const val LOGIN = "login"
    const val CALLBACK = "callback"
    const val SELF = "self"
    const val USERNAME = "{username}"
    const val REFRESH = "refresh"
    const val LOGOUT = "logout"
    const val DELETE = "delete"

    const val IMPORT = "import"
    const val AVAILABLE = "available"

    const val START = "start"
    const val STATS = "stats"

    const val USER_ID = "{uid}"
    const val TRAIL_ID = "{tid}"
    const val HIKE_ID = "{hid}"

    const val GET_TRAILS = "$USER_ID/$TRAILS"

    const val TRAIL_START = "$TRAIL_ID/$START"
    const val GET_STATS = "$USER_ID/$STATS"

    const val USERS_CREATE = "$USERS/$CREATE" // POST
    const val USERS_LOGIN = "$USERS/$LOGIN" // POST
    const val USERS_OAUTH_LOGIN = "$USERS/$OAUTH/$GOOGLE" // GET
    const val USERS_OAUTH_CALLBACK = "$USERS/$OAUTH/$CALLBACK" // GET
    const val USERS_SELF = "$USERS/$SELF" // GET

    fun routeGetUserByName(username: String) = "$USERS/$username" // GET

    const val USERS_REFRESH = "$USERS/$REFRESH" // PUT
    const val USERS_LOGOUT = "$USERS/$LOGOUT" // DELETE
    const val USERS_DELETE = "$USERS/$DELETE" // DELETE

    const val TRAILS_CREATE = "$TRAILS/$CREATE" // POST
    const val TRAILS_IMPORT = "$TRAILS/$IMPORT" // POST
    const val TRAILS_AVAILABLE = "$TRAILS/$AVAILABLE" // GET

    fun routeTrailId(id: ULong) = "$TRAILS/$id" // GET | PUT | DELETE

    fun routeUserTrails(id: ULong) = "$USERS/$id/$TRAILS" // GET

    fun routeTrailStart(id: ULong) = "$TRAILS/$id/$START" // GET

    fun routeGetHike(id: ULong) = "$HIKES/$id" // GET

    fun routeHikeTrailId(id: ULong) = "$HIKES/$id" // PUT | DELETE

    fun routeUserStats(id: ULong) = "$USERS/$id/$STATS" // GET
}
