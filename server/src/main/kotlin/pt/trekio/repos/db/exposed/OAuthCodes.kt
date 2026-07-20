package pt.trekio.repos.db.exposed

import org.jetbrains.exposed.v1.core.Table

object OAuthCodes : Table("oauth_codes") {
    val email = text("email")
    val username = text("username")
    val code = text("code").uniqueIndex()
    val expiredAt = long("expired_at")
    override val primaryKey = PrimaryKey(code)
}
