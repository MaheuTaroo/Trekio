package pt.trekio.misc

import pt.trekio.misc.Routes.AVAILABLE
import pt.trekio.misc.Routes.CALLBACK
import pt.trekio.misc.Routes.CREATE
import pt.trekio.misc.Routes.DELETE
import pt.trekio.misc.Routes.DOCS
import pt.trekio.misc.Routes.GOOGLE
import pt.trekio.misc.Routes.HIKES
import pt.trekio.misc.Routes.HIKE_ID
import pt.trekio.misc.Routes.IMPORT
import pt.trekio.misc.Routes.LOGIN
import pt.trekio.misc.Routes.LOGOUT
import pt.trekio.misc.Routes.OAUTH
import pt.trekio.misc.Routes.REFRESH
import pt.trekio.misc.Routes.SELF
import pt.trekio.misc.Routes.START
import pt.trekio.misc.Routes.STATS
import pt.trekio.misc.Routes.TRAILS
import pt.trekio.misc.Routes.TRAIL_ID
import pt.trekio.misc.Routes.USERNAME
import pt.trekio.misc.Routes.USERS
import pt.trekio.misc.Routes.USER_ID

object Routes {
    const val BASE_URL = "https://takisha-unsustaining-unceasingly.ngrok-free.dev"

    const val DOCS = "/docs"

    const val USERS = "/users"
    const val TRAILS = "/trails"
    const val HIKES = "/hikes"
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
}

sealed class ApiRoutes(
    val path: String,
    val requireAuthType: AuthType,
) {
    object Docs : ApiRoutes(DOCS, AuthType.NONE)

    object UserCreate : ApiRoutes("$USERS/$CREATE", AuthType.NONE) // POST

    object UserLogin : ApiRoutes("$USERS/$LOGIN", AuthType.NONE) // POST

    object UserOauthLogin : ApiRoutes("$USERS/$OAUTH/$GOOGLE", AuthType.OAUTH) // GET

    object UserOauthCallback : ApiRoutes("$USERS/$OAUTH/$CALLBACK", AuthType.OAUTH) // GET

    object Users : ApiRoutes(USERS, AuthType.JWT) // GET

    object UserSelf : ApiRoutes("$USERS/$SELF", AuthType.JWT) // GET

    class UserByUsername(
        val username: String? = null,
    ) : ApiRoutes(USERS + (username ?: USERNAME), AuthType.JWT) // GET

    object UserRefresh : ApiRoutes("$USERS/$REFRESH", AuthType.BEARER) // PUT

    object UserLogout : ApiRoutes("$USERS/$LOGOUT", AuthType.BEARER) // DELETE

    object UserDelete : ApiRoutes("$USERS/$DELETE", AuthType.BEARER) // DELETE

    object TrailCreate : ApiRoutes("$TRAILS/$CREATE", AuthType.JWT) // POST

    object TrailsImport : ApiRoutes("$TRAILS/$IMPORT", AuthType.JWT) // POST

    object TrailsAvailable : ApiRoutes("$TRAILS/$AVAILABLE", AuthType.JWT) // GET

    class TrailById(
        val id: ULong? = null,
    ) : ApiRoutes(TRAILS + (id ?: TRAIL_ID), AuthType.JWT) // GET

    class TrailUpdate(
        val id: ULong? = null,
    ) : ApiRoutes(TRAILS + (id ?: TRAIL_ID), AuthType.JWT) // PUT

    class TrailDelete(
        val id: ULong? = null,
    ) : ApiRoutes(TRAILS + (id ?: TRAIL_ID), AuthType.JWT) // DELETE

    class UserTrails(
        val id: ULong? = null,
    ) : ApiRoutes(USERS + (id ?: TRAIL_ID) + TRAILS, AuthType.JWT) // GET

    class TrailStart(
        val id: ULong? = null,
    ) : ApiRoutes(TRAILS + (id ?: TRAIL_ID) + START, AuthType.JWT) // GET

    class HikeById(
        val id: ULong? = null,
    ) : ApiRoutes(HIKES + (id ?: HIKE_ID), AuthType.JWT) // GET

    class HikeFinishByTrailId(
        val id: ULong? = null,
    ) : ApiRoutes(HIKES + (id ?: TRAIL_ID), AuthType.JWT) // PUT

    class HikeCancelTrail(
        val id: ULong? = null,
    ) : ApiRoutes(HIKES + (id ?: TRAIL_ID), AuthType.JWT) // DELETE

    class HikeUserStats(
        val id: ULong? = null,
    ) : ApiRoutes(USERS + (id ?: USER_ID) + STATS, AuthType.JWT) // GET
}
