package pt.trekio.repos.db.exposed

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.ULongIdTable

object Hikes : ULongIdTable() {
    val hiker = reference("hiker", Users.id, onDelete = ReferenceOption.CASCADE)
    val trail = reference("hiked_trail", Trails.id, onDelete = ReferenceOption.CASCADE)
    val entry = geoPoint("entry")
    val exit = geoPoint("exit").nullable().default(null)
    val start = long("start_time")
    val finish = long("end_time").nullable().default(null)
}
