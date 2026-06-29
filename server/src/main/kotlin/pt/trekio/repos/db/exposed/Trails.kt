package pt.trekio.repos.db.exposed

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.ULongIdTable
import pt.trekio.misc.TrailDifficulty

object Trails : ULongIdTable("trails") {
    val name = text("trail_name").uniqueIndex()
    val creator = reference("creator", Users.id, onDelete = ReferenceOption.CASCADE)
    val startingPoint = geoPoint("start")
    val endingPoint = geoPoint("end")
    val path = array("path", GeoPointColumnType())
    val distance = double("distance")
    val difficulty = enumeration<TrailDifficulty>("difficulty")
    val parent = reference("parent_trail", id, onDelete = ReferenceOption.CASCADE).nullable()
}
