package pt.trekio.repos.db.exposed

import org.jetbrains.exposed.v1.core.dao.id.ULongIdTable
import pt.trekio.misc.UserRank

object Users : ULongIdTable("users") {
    val username = text("username").uniqueIndex()
    val email = text("email").uniqueIndex()
    val passwordValidation = text("pass_hash").nullable()
    val rank = enumeration("rank", UserRank::class).default(UserRank.NEW)
}
