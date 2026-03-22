package pt.trekio.repos.db.exposed

import org.jetbrains.exposed.v1.core.Table
import pt.trekio.misc.UserRank

object Users : Table("users") {
    val username = text("username")
    val email = text("email").nullable()
    val passwordValidation = text("pass_hash")
    val rank = enumeration("rank", UserRank::class).default(UserRank.NEW)
    val trails = integer("completed_trails").default(0)
    val totalKms = double("total_kms").default(0.0)
    val hikingTime = long("hiking_time").default(0)

    override val primaryKey = PrimaryKey(username)
}
