package pt.trekio.repos.db.exposed

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object Tokens : Table("tokens") {
    val uid = reference("uid", Users.id, onDelete = ReferenceOption.CASCADE)
    val tokenValidation = text("token_hash")
    val lastUse = long("last_use")

    override val primaryKey = PrimaryKey(uid, tokenValidation)
}
