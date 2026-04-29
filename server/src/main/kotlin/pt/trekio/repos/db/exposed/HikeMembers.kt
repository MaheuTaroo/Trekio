package pt.trekio.repos.db.exposed

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object HikeMembers : Table("hike_members") {
    val hikeId = reference("hike_id", Hikes.id, onDelete = ReferenceOption.CASCADE)
    val hikerId = reference("hiker_id", Users.id, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val currentLocation = geoPoint("curr_location")

    override val primaryKey = PrimaryKey(hikeId, hikerId)
}
