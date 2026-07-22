package pt.trekio.repos.db

import org.postgresql.ds.PGSimpleDataSource

internal val DRIVER_NAME = PGSimpleDataSource::class.qualifiedName!!
internal const val USER_DB_INIT_LOCK_ID = 1578632L // Completely random number
internal const val TRAIL_DB_INIT_LOCK_ID = 1578633L // Completely random number
internal const val HIKE_DB_INIT_LOCK_ID = 1578634L // Completely random number
