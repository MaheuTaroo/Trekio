package pt.trekio.repos.db.exposed

import org.jetbrains.exposed.v1.core.Table

object Tokens : Table("tokens") {
    val username = text("username").references(Users.username)
    val tokenValidation = text("token_hash")
    val lastUse = long("last_use")

    override val primaryKey = PrimaryKey(username, tokenValidation)
}
