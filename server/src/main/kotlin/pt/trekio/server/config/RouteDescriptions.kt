package pt.trekio.server.config

import io.ktor.http.HttpStatusCode
import io.ktor.openapi.JsonSchema
import io.ktor.openapi.JsonSchemaInference
import io.ktor.openapi.Operation
import io.ktor.openapi.Responses
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi
import pt.trekio.dto.ErrorMessage
import pt.trekio.dto.ResultIdDto
import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.dto.TrailCreate
import pt.trekio.dto.TrailDto
import pt.trekio.dto.TrailListDto
import pt.trekio.dto.UserCreate
import pt.trekio.dto.UserCredentialLogin
import pt.trekio.dto.UserList

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

    private fun Operation.Builder.requireSecurity() {
        security {
            requirement("trekio-bearer")
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

    private fun Responses.Builder.noContent(text: String) =
        HttpStatusCode.NoContent {
            description = text
        }

    private fun Responses.Builder.badRequest(cause: String) =
        HttpStatusCode.BadRequest {
            description = cause
            schema = ERROR_SCHEMA
        }

    private fun Responses.Builder.unauthorized() =
        HttpStatusCode.Unauthorized {
            description = "Authentication failure."
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

    object Users {
        private const val TAG = "Users"

        fun Route.describeUserCreation() =
            applyDescription(TAG, "Registration", "Registers a new user.") {
                requestBody {
                    required = true
                    schema = jsonSchema<UserCreate>()
                }

                responses {
                    created<TokenExternalInfoDto>("The newly created user's token.")

                    badRequest("Either username, email or password does not follow specific format.")

                    unauthorized()

                    HttpStatusCode.Conflict {
                        description = "User creation failure due to repeated username or email."
                        schema = ERROR_SCHEMA
                    }
                }
            }

        fun Route.describeUserList() =
            applyDescription(
                TAG,
                "List",
                "Fetches a page of users following skipping and limiting values.",
            ) {
                requireSecurity()

                requirePagination()

                responses {
                    ok<UserList>("The paginated list of users.")

                    badRequest("Negative skip or limit value.")

                    unauthorized()
                }
            }

        fun Route.describeUserInfo() =
            applyDescription(TAG, "Self", "Fetches the current user's details.") {
                requireSecurity()

                responses {
                    ok<UserList>("The user's own details.")

                    unauthorized()
                }
            }

        fun Route.describeUserByName() =
            applyDescription(TAG, "Info", "Fetches the details of a user by their name.") {
                requireSecurity()

                dynamicPath("name", "The user's name.")

                responses {
                    ok<UserList>("The paginated list of users.")

                    badRequest("Username does not follow format.")

                    unauthorized()

                    notFound("Username not associated to user.")
                }
            }

        fun Route.describeUserDeletion() =
            applyDescription(TAG, "Deletion", "Removes the user's own account.") {
                requireSecurity()

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
                    created<TokenExternalInfoDto>("The user's new token.")

                    badRequest("Email does not follow format.")

                    forbidden("Incorrect password.")

                    notFound("Email not associated to user.")
                }
            }

        fun Route.describeLogout() =
            applyDescription(TAG, "Logout", "Logs a user out.") {
                requireSecurity()

                responses {
                    noContent("Log out success.")

                    unauthorized()
                }
            }

        fun Route.describeRefreshToken() =
            applyDescription(TAG, "Refresh", "Recreates new access and refresh tokens.") {
                requireSecurity()

                responses {
                    ok<TokenExternalInfoDto>("The user's new tokens.")

                    unauthorized()

                    notFound("User not found.")
                }
            }
    }

    object Trails {
        private const val TAG = "Trails"

        fun Route.describeTrailCreation() =
            applyDescription(TAG, "Creation", "Creates a new trail.") {
                requireSecurity()

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
                requireSecurity()

                responses {
                    created<ResultIdDto>("The new trail's ID.")

                    unauthorized()

                    notFound("The user was not found.")

                    badRequest("The user or trail names are invalid, or the trail data is incorrectly formed.")
                }
            }

        fun Route.describeAvailableTrails() =
            applyDescription(TAG, "All", "Fetches all available trails.") {
                requireSecurity()

                requirePagination()

                responses {
                    ok<TrailListDto>("The paginates list of available trails.")

                    unauthorized()

                    badRequest("Negative skip or limit value.")
                }
            }

        fun Route.describeUserTrails() =
            applyDescription(TAG, "User-made", "Fetches the user's trails.") {
                requireSecurity()

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
                requireSecurity()

                dynamicPath("tid", "The trail's ID.")

                responses {
                    ok<TrailDto>("The trail's details.")

                    unauthorized()

                    notFound("Invalid trail ID.")
                }
            }

        fun Route.describeTrailUpdate() =
            applyDescription(TAG, "Update", "Updates a trail.") {
                requireSecurity()

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
                requireSecurity()

                dynamicPath("tid", "The trail's ID.")

                responses {
                    noContent("Trail deletion success.")

                    unauthorized()

                    notFound("Invalid trail ID.")

                    forbidden("Trail not owned by user.")
                }
            }
    }
}
