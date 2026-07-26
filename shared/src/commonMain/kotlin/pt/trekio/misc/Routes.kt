package pt.trekio.misc

import pt.trekio.misc.Routes.AVAILABLE
import pt.trekio.misc.Routes.BY_ID
import pt.trekio.misc.Routes.BY_NAME
import pt.trekio.misc.Routes.CALLBACK
import pt.trekio.misc.Routes.CODE
import pt.trekio.misc.Routes.CREATE
import pt.trekio.misc.Routes.DELETE
import pt.trekio.misc.Routes.DOCS
import pt.trekio.misc.Routes.GOOGLE
import pt.trekio.misc.Routes.HIKES
import pt.trekio.misc.Routes.IMPORT
import pt.trekio.misc.Routes.LOGIN
import pt.trekio.misc.Routes.LOGOUT
import pt.trekio.misc.Routes.MINE
import pt.trekio.misc.Routes.OAUTH
import pt.trekio.misc.Routes.REFRESH
import pt.trekio.misc.Routes.SELF
import pt.trekio.misc.Routes.START
import pt.trekio.misc.Routes.STATS
import pt.trekio.misc.Routes.TRAILS
import pt.trekio.misc.Routes.TRAIL_ID
import pt.trekio.misc.Routes.TRAIL_IS_FIRST_POINT
import pt.trekio.misc.Routes.TRAIL_NAME
import pt.trekio.misc.Routes.TREKIO
import pt.trekio.misc.Routes.UPDATE
import pt.trekio.misc.Routes.USERS
import pt.trekio.misc.Routes.USER_ID
import pt.trekio.misc.Routes.USER_IDENTIFIER

object Routes {
    const val BASE_URL = "https://postbursal-bernardina-unconscientiously.ngrok-free.dev"
    const val OAUTH_INFO = "https://www.googleapis.com/oauth2/v2/userinfo"
    const val OAUTH_AUTH = "https://accounts.google.com/o/oauth2/auth"
    const val OAUTH_TOKEN = "https://accounts.google.com/o/oauth2/token"
    const val OAUTH_EMAIL = "https://www.googleapis.com/auth/userinfo.email"

    const val TREKIO = "trekio"
    const val CODE = "code"
    const val EMAIL = "email"
    const val USERNAME = "username"
    const val NEW = "new"
    const val ERROR = "error"

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
    const val USER_IDENTIFIER = "{identifier}"
    const val REFRESH = "refresh"
    const val UPDATE = "update"
    const val LOGOUT = "logout"
    const val DELETE = "delete"

    const val IMPORT = "import"
    const val AVAILABLE = "available"
    const val BY_NAME = "name"
    const val BY_ID = "id"

    const val START = "start"
    const val MINE = "mine"
    const val STATS = "stats"

    const val USER_ID = "{uid}"
    const val TRAIL_ID = "{tid}"
    const val TRAIL_NAME = "{tname}"
    const val TRAIL_IS_FIRST_POINT = "{isFirstPoint}"
    const val HIKE_ID = "{hid}"
}

sealed class ApiRoutes(
    val path: String,
    val requireAuthType: AuthType,
) {
    data object DeepLink : ApiRoutes("$TREKIO://$OAUTH/$CALLBACK", AuthType.NONE)

    data object Docs : ApiRoutes(DOCS, AuthType.NONE)

    data object UserCreate : ApiRoutes("$USERS/$CREATE", AuthType.NONE) // POST

    data object UserLogin : ApiRoutes("$USERS/$LOGIN", AuthType.NONE) // POST

    data object UserOAuthCodeVerifier : ApiRoutes("$USERS/$OAUTH/$CODE", AuthType.NONE) // POST

    data object UserOauthLogin : ApiRoutes("$USERS/$OAUTH/$GOOGLE", AuthType.OAUTH) // GET

    data object UserOauthCallback : ApiRoutes("$USERS/$OAUTH/$CALLBACK", AuthType.OAUTH) // GET

    data object Users : ApiRoutes(USERS, AuthType.JWT) // GET

    data object UserSelf : ApiRoutes("$USERS/$SELF", AuthType.JWT) // GET

    data class UserByIdentifier(
        val identifier: String? = null,
    ) : ApiRoutes("$USERS/${identifier ?: USER_IDENTIFIER}", AuthType.JWT) // GET

    data object UserRefresh : ApiRoutes("$USERS/$REFRESH", AuthType.BEARER) // PUT

    data object UserUpdate : ApiRoutes("$USERS/$UPDATE", AuthType.BEARER) // PUT

    data object UserLogout : ApiRoutes("$USERS/$LOGOUT", AuthType.BEARER) // DELETE

    data object UserDelete : ApiRoutes("$USERS/$DELETE", AuthType.BEARER) // DELETE

    data object TrailCreate : ApiRoutes("$TRAILS/$CREATE", AuthType.JWT) // POST

    data object TrailsImport : ApiRoutes("$TRAILS/$IMPORT", AuthType.JWT) // POST

    data object TrailsAvailable : ApiRoutes("$TRAILS/$AVAILABLE", AuthType.JWT) // GET

    data class TrailById(
        val id: ULong? = null,
    ) : ApiRoutes("$TRAILS/$BY_ID/${id ?: TRAIL_ID}", AuthType.JWT) // GET

    data class TrailsByName(
        val name: String? = null,
    ) : ApiRoutes("$TRAILS/$BY_NAME/${name ?: TRAIL_NAME}", AuthType.JWT) // GET

    data class TrailUpdate(
        val id: ULong? = null,
    ) : ApiRoutes("$TRAILS/${id ?: TRAIL_ID}", AuthType.JWT) // PUT

    data class TrailDelete(
        val id: ULong? = null,
    ) : ApiRoutes("$TRAILS/${id ?: TRAIL_ID}", AuthType.JWT) // DELETE

    data class UserTrails(
        val id: ULong? = null,
    ) : ApiRoutes("$USERS/${id ?: USER_ID}/$TRAILS", AuthType.JWT) // GET

    data class TrailStart(
        val id: ULong? = null,
        val isFirstPoint: Boolean? = null,
    ) : ApiRoutes("$TRAILS/${id ?: TRAIL_ID}/$START/${isFirstPoint ?: TRAIL_IS_FIRST_POINT}", AuthType.JWT) // GET

    data object HikesBySelf : ApiRoutes("$HIKES/$MINE", AuthType.JWT) // GET

    data class HikeUserStats(
        val id: ULong? = null,
    ) : ApiRoutes("$USERS/${id ?: USER_ID}/$STATS", AuthType.JWT) // GET
}
