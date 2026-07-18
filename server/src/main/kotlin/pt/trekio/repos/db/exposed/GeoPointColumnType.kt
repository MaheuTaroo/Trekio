package pt.trekio.repos.db.exposed

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.toGeoPoint

class GeoPointColumnType : ColumnType<GeoPoint>() {
    private fun Any.tryParseToGeoPoint(): GeoPoint? {
        if (this is GeoPoint) return this
        if (this !is String) {
            error(
                "Cannot convert value to GeoPoint (must be textual, is ${
                    this::class.qualifiedName ?: "either a local class or an anonymous object"
                })",
            )
        }

        val value = trim()

        if (value.lowercase() == "null") {
            return null
        }

        return value.toGeoPoint()
    }

    override fun sqlType() = "TEXT"

    override fun valueFromDB(value: Any) = value.tryParseToGeoPoint()

    override fun setParameter(
        stmt: PreparedStatementApi,
        index: Int,
        value: Any?,
    ) {
        require(value is GeoPoint?) { "value must be GeoPoint or null" }
        super.setParameter(stmt, index, value?.toString())
    }
}

fun Table.geoPoint(name: String): Column<GeoPoint> = registerColumn(name, GeoPointColumnType())
