package pt.trekio.repos.db.exposed

import org.jetbrains.exposed.v1.core.dao.id.ULongIdTable

object Hikes : ULongIdTable() {
    val hiker = reference("hiker", Users.id)
    val trail = reference("hiked_trail", Trails.id)
    val entry = geoPoint("entry")
    val exit = geoPoint("exit").nullable()
    val start = ulong("start_time")
    val finish = ulong("end_time").nullable()
}
