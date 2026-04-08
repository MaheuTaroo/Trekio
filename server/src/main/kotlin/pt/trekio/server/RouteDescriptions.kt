package pt.trekio.server

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
import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.dto.UserCredentialLogin
import pt.trekio.dto.UserList

@OptIn(ExperimentalKtorApi::class)
object RouteDescriptions {
    private val JsonSchemaInference.ERROR_SCHEMA: JsonSchema
        get() = jsonSchema<ErrorMessage>()

    private fun Route.applyDescription(config: Operation.Builder.() -> Unit): Route {
        describe(config)
        return this
    }

    private fun Operation.Builder.trekioSecurity() {
        security {
            requirement("trekio-bearer")
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

    fun Route.describeUserCreation() =
        applyDescription {
            tag("Users")

            operationId = "Registration"
            description = "Registers a new user."

            requestBody {
                required = true
                schema = jsonSchema<TokenExternalInfoDto>()
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
        applyDescription {
            tag("Users")

            operationId = "User List"
            description = "Fetches a page of users following skipping and limiting values."

            trekioSecurity()

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

            responses {
                ok<UserList>("The paginated list of users.")

                badRequest("Negative skip or limit value.")

                unauthorized()
            }
        }

    fun Route.describeUserInfo() =
        applyDescription {
            tag("Users")

            operationId = "Own Details"
            description = "Fetches the current user's details."

            trekioSecurity()

            responses {
                ok<UserList>("The user's own details.")

                unauthorized()
            }
        }

    fun Route.describeUserByName() =
        applyDescription {
            tag("Users")

            operationId = "User Details"
            description = "Fetches the details of a user by their name."

            trekioSecurity()

            parameters {
                path("name") {
                    required = true
                    description = "The name of the user to fetch."
                }
            }

            responses {
                ok<UserList>("The paginated list of users.")

                badRequest("Username does not follow format.")

                unauthorized()

                notFound("Username not associated to user.")
            }
        }

    fun Route.describeUserDeletion() =
        applyDescription {
            tag("Users")

            operationId = "Deletion"
            description = "Removes the user's own account."

            trekioSecurity()

            responses {
                noContent("User deletion success.")

                unauthorized()
            }
        }

    fun Route.describeLogin() =
        applyDescription {
            tag("Users")

            operationId = "Login"
            description = "Logs a user in."

            requestBody {
                required = true
                schema = jsonSchema<UserCredentialLogin>()
            }

            responses {
                created<TokenExternalInfoDto>("The user's new token.")

                badRequest("Email does not follow format.")

                HttpStatusCode.Forbidden {
                    description = "Incorrect password."
                    schema = ERROR_SCHEMA
                }

                notFound("Email not associated to user.")
            }
        }

    fun Route.describeLogout() =
        applyDescription {
            tag("Users")

            operationId = "Logout"
            description = "Logs a user out."

            trekioSecurity()

            responses {
                noContent("Log out success.")

                unauthorized()
            }
        }
}
