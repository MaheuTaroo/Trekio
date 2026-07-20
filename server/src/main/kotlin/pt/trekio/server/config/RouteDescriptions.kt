package pt.trekio.server.config

import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.openapi.JsonSchema
import io.ktor.openapi.JsonSchemaInference
import io.ktor.openapi.Operation
import io.ktor.openapi.Responses
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi
import pt.trekio.dto.ErrorMessage
import pt.trekio.dto.HikeDto
import pt.trekio.dto.OAuthCodeDto
import pt.trekio.dto.ResultIdDto
import pt.trekio.dto.StatisticsDto
import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.dto.TrailCreate
import pt.trekio.dto.TrailDto
import pt.trekio.dto.TrailListDto
import pt.trekio.dto.UserCreateDto
import pt.trekio.dto.UserCredentialLogin
import pt.trekio.dto.UserDto
import pt.trekio.dto.UserList
import pt.trekio.server.BEARER_SCHEME
import pt.trekio.server.JWT_SCHEME
import pt.trekio.server.OAUTH_SCHEME
import pt.trekio.server.config.RouteDescriptions.found
import pt.trekio.server.config.RouteDescriptions.ok

@OptIn(ExperimentalKtorApi::class)
object RouteDescriptions {
    private val JsonSchemaInference.ERROR_SCHEMA: JsonSchema
        get() = jsonSchema<ErrorMessage>()

    private fun Route.applyDescription(
        tag: String,
        operId: String,
        text: String,
        config: Operation.Builder.() -> Unit,
    ): Route {
        describe {
            tag(tag)

            operationId = operId
            description = text

            config()
        }
        return this
    }

    private fun Operation.Builder.requireSecurityJwt() {
        security {
            requirement(JWT_SCHEME)
        }
    }

    private fun Operation.Builder.requireSecurityBearer() {
        security {
            requirement(BEARER_SCHEME)
        }
    }

    private fun Operation.Builder.requireSecurityOAuth() {
        security {
            requirement(OAUTH_SCHEME)
        }
    }

    private fun Operation.Builder.requirePagination() {
        parameters {
            query("skip") {
                description = "The amount of users to skip."
                required = false
            }

            query("limit") {
                description = "The number of users to fetch."
                required = false
            }
        }
    }

    private fun Operation.Builder.dynamicPath(
        name: String,
        text: String,
    ) {
        parameters {
            path(name) {
                required = true
                description = text
            }
        }
    }

    private inline fun <reified T : Any> Responses.Builder.ok(text: String) =
        HttpStatusCode.OK {
            description = text
            schema = jsonSchema<T>()
        }

    private inline fun <reified T : Any> Responses.Builder.created(text: String) =
        HttpStatusCode.Created {
            description = text
            schema = jsonSchema<T>()
        }

    private fun Responses.Builder.found(
        text: String,
        deepLink: String,
    ) = HttpStatusCode.Found {
        description = text
        this.headers {
            headersOf("Location", deepLink)
        }
    }

    private fun Responses.Builder.noContent(text: String) =
        HttpStatusCode.NoContent {
            description = text
        }

    private fun Responses.Builder.badRequest(cause: String) =
        HttpStatusCode.BadRequest {
            description = cause
            schema = ERROR_SCHEMA
        }

    private fun Responses.Builder.unauthorized(cause: String? = null) =
        HttpStatusCode.Unauthorized {
            description = cause ?: "Authentication failure."
            schema = ERROR_SCHEMA
        }

    private fun Responses.Builder.notFound(cause: String) =
        HttpStatusCode.NotFound {
            description = cause
            schema = ERROR_SCHEMA
        }

    private fun Responses.Builder.forbidden(cause: String) =
        HttpStatusCode.Forbidden {
            description = cause
            schema = ERROR_SCHEMA
        }

    private fun Responses.Builder.conflict(cause: String) =
        HttpStatusCode.Conflict {
            description = cause
            schema = ERROR_SCHEMA
        }

    object Users {
        private const val TAG = "Users"

        fun Route.describeUserCreation() =
            applyDescription(TAG, "Registration", "Registers a new user.") {
                requestBody {
                    required = true
                    schema = jsonSchema<UserCreateDto>()
                }

                responses {
                    created<TokenExternalInfoDto>("The newly created user's tokens.")

                    badRequest("Either username, email or password does not follow specific format.")

                    badRequest("Email already in use with OAuth.")

                    unauthorized()

                    conflict("User creation failure due to repeated username or email.")
                }
            }

        fun Route.describeUserList() =
            applyDescription(
                TAG,
                "List",
                "Fetches a page of users following skipping and limiting values.",
            ) {
                requireSecurityJwt()

                requirePagination()

                responses {
                    ok<UserList>("The paginated list of users.")

                    badRequest("Negative skip or limit value.")

                    unauthorized()
                }
            }

        fun Route.describeUserInfo() =
            applyDescription(TAG, "Self", "Fetches the current user's details.") {
                requireSecurityJwt()

                responses {
                    ok<UserList>("The user's own details.")

                    unauthorized()
                }
            }

        fun Route.describeUserByIdentifier() =
            applyDescription(
                TAG,
                "Info",
                "Fetches the details of a user by their identifier (username or ID).",
            ) {
                requireSecurityJwt()

                dynamicPath("identifier", "The user's identifier (username or ID).")

                responses {
                    ok<UserDto>("The specified user's information.")

                    badRequest("Identifier is not numeric or a valid username.")

                    unauthorized()

                    notFound("Identifier not associated to user.")
                }
            }

        fun Route.describeUserDeletion() =
            applyDescription(TAG, "Deletion", "Removes the user's own account.") {
                requireSecurityBearer()

                responses {
                    noContent("User deletion success.")

                    unauthorized()
                }
            }

        fun Route.describeLogin() =
            applyDescription(TAG, "Login", "Logs a user in.") {
                requestBody {
                    required = true
                    schema = jsonSchema<UserCredentialLogin>()
                }

                responses {
                    created<TokenExternalInfoDto>("The user's new tokens.")

                    badRequest("Email does not follow format, or is already in use with OAuth.")

                    forbidden("Incorrect password.")

                    notFound("Email not associated to user.")
                }
            }

        fun Route.describeLogout() =
            applyDescription(TAG, "Logout", "Logs a user out.") {
                requireSecurityBearer()

                responses {
                    noContent("Log out success.")

                    unauthorized()
                }
            }

        fun Route.describeRefreshToken() =
            applyDescription(TAG, "Refresh", "Recreates new access and refresh tokens.") {
                requireSecurityBearer()

                responses {
                    ok<TokenExternalInfoDto>("The user's new tokens.")

                    unauthorized()

                    notFound("User was not found.")
                }
            }

        fun Route.describeUpdate() =
            applyDescription(TAG, "Update", "Update the user's details.") {
                requireSecurityBearer()

                responses {
                    created<TokenExternalInfoDto>("The user's new token.")

                    badRequest(
                        "Either username or password does not follow specific format, or username is repeated.",
                    )

                    unauthorized()
                }
            }

        fun Route.describeOAuth() =
            applyDescription(TAG, "OAuth", "Sign up or login with OAuth.") {
                requireSecurityOAuth()

                responses {
                    created<TokenExternalInfoDto>("The user's new token.")

                    notFound("User was not found.")

                    unauthorized("Google information is inaccessible.")

                    badRequest(
                        "Email does not follow format, or there is an account with the given username or email.",
                    )
                }
            }

        fun Route.describeOAuthCallback() =
            applyDescription(TAG, "OAuthCallback", "Gets the OAuth callback and redirects client with deep link.") {
                requireSecurityOAuth()

                responses {
                    found("Returns a non-permanent deep link to the client.", "trekio://oauth/callback?code=...&email=...&username=...")

                    unauthorized("Couldn't retrieve user email from Google.")

                    badRequest("Invalid email.")
                }
            }

        fun Route.describeOAuthVerifier() =
            applyDescription(TAG, "OAuth Verifier", "Verifies de code from deep link and logins user.") {
                requestBody {
                    required = true
                    schema = jsonSchema<OAuthCodeDto>()
                }

                responses {
                    created<TokenExternalInfoDto>("The user's new tokens.")

                    badRequest("The user's code probably expired, or code, email or username is invalid or missing.")

                    unauthorized("The user token is invalid.")
                }
            }

        fun Route.describeUserStatistics() =
            applyDescription(TAG, "Statistics", "Retrieves the user's statistics.") {
                requireSecurityJwt()

                dynamicPath("uid", "The user's ID.")

                responses {
                    ok<StatisticsDto>("Statistics retrieval success.")

                    unauthorized()
                }
            }
    }

    object Trails {
        private const val TAG = "Trails"

        fun Route.describeTrailCreation() =
            applyDescription(TAG, "Creation", "Creates a new trail.") {
                requireSecurityJwt()

                requestBody {
                    required = true
                    schema = jsonSchema<TrailCreate>()
                }

                responses {
                    created<ResultIdDto>("Trail creation success.")

                    unauthorized()

                    badRequest("Invalid trail name.")

                    notFound("Invalid parent trail ID.")
                }
            }

        fun Route.describeTrailImport() =
            applyDescription(
                TAG,
                "Import",
                "Imports a KML trail. ALLOWED CONTENT-TYPE HEADERS: " +
                    "application/vnd.google-earth.kml+xml, application/xml, text/xml",
            ) {
                requireSecurityJwt()

                responses {
                    created<ResultIdDto>("The new trail's ID.")

                    unauthorized()

                    notFound("The user was not found.")

                    badRequest("The user or trail names are invalid, or the trail data is incorrectly formed.")
                }
            }

        fun Route.describeAvailableTrails() =
            applyDescription(TAG, "All", "Fetches all available trails.") {
                requireSecurityJwt()

                requirePagination()

                responses {
                    ok<TrailListDto>("The paginates list of available trails.")

                    unauthorized()

                    badRequest("Negative skip or limit value.")
                }
            }

        fun Route.describeUserTrails() =
            applyDescription(TAG, "User-made", "Fetches the user's trails.") {
                requireSecurityJwt()

                requirePagination()

                responses {
                    ok<TrailListDto>("The user's trails.")

                    unauthorized()

                    notFound("User not found.")

                    badRequest("Negative skip or limit value.")
                }
            }

        fun Route.describeSpecificTrail() =
            applyDescription(TAG, "Specific", "Fetches a specific trail.") {
                requireSecurityJwt()

                dynamicPath("tid", "The trail's ID.")

                responses {
                    ok<TrailDto>("The trail's details.")

                    unauthorized()

                    notFound("Invalid trail ID.")
                }
            }

        fun Route.describeTrailUpdate() =
            applyDescription(TAG, "Update", "Updates a trail.") {
                requireSecurityJwt()

                dynamicPath("tid", "The trail's ID.")

                responses {
                    noContent("Trail update success.")

                    unauthorized()

                    forbidden("Trail not owned by user.")

                    notFound("Invalid trail or parent trail ID.")

                    badRequest("Invalid trail name, or invalid parent trail.")
                }
            }

        fun Route.describeTrailDeletion() =
            applyDescription(TAG, "Deletion", "Deletes a trail.") {
                requireSecurityJwt()

                dynamicPath("tid", "The trail's ID.")

                responses {
                    noContent("Trail deletion success.")

                    unauthorized()

                    notFound("Invalid trail ID.")

                    forbidden("Trail not owned by user.")
                }
            }
    }

    object Hikes {
        private const val TAG = "Hikes"

        fun Route.describeHikeDetails() =
            applyDescription(TAG, "Details", "Shows the details of a hike.") {
                requireSecurityJwt()

                dynamicPath("hid", "The hike's ID.")

                responses {
                    ok<HikeDto>("The hike's details.")

                    notFound("Invalid hike ID.")

                    unauthorized("Hike not done by user.")
                }
            }

        fun Route.describeHikeFinish() =
            applyDescription(TAG, "Finish", "Marks a hike as finished.") {
                requireSecurityJwt()

                dynamicPath("hid", "The hike's ID.")

                responses {
                    noContent("Trail finish success.")

                    notFound("Invalid hike ID or user ID.")

                    badRequest("Invalid ending point, or user is either not hiking or not on specified hike.")

                    unauthorized("Hike not done by user.")
                }
            }

        fun Route.describeHikeCancel() =
            applyDescription(TAG, "Cancellation", "Cancels a hike.") {
                requireSecurityJwt()

                dynamicPath("hid", "The hike's ID.")

                responses {
                    noContent("Hike cancellation success.")

                    notFound("Invalid hike ID.")

                    badRequest("User is either not hiking, or not on specified hike.")
                }
            }
    }
}
