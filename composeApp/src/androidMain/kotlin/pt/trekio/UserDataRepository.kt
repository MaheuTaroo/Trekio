package pt.trekio

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.IOException
import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.first
import pt.trekio.misc.UserAndToken
import pt.trekio.misc.UserDetailsAndToken
import pt.trekio.repos.UserRepository

fun createDataStore(storage: Storage<Preferences>): DataStore<Preferences> = DataStoreFactory.create(storage = storage)

internal const val DATASTORE_FILENAME = "trekio.preferences_pb"

class UserDataRepository(
    private val store: DataStore<Preferences>,
) : UserRepository {
    private val accessTokenKey = stringPreferencesKey("accessToken")
    private val refreshTokenKey = stringPreferencesKey("refreshToken")
    private val emailKey = stringPreferencesKey("email")
    private val expirKey = longPreferencesKey("expiration")
    private val idKey = longPreferencesKey("id")
    private val userNameKey = stringPreferencesKey("userName")
    private val rankKey = stringPreferencesKey("rank")

    override suspend fun saveToken(
        accessToken: String,
        refreshToken: String,
        expiration: Long,
        email: String?,
    ) {
        store.edit {
            it[accessTokenKey] = accessToken
            it[refreshTokenKey] = refreshToken
            it[expirKey] = expiration
            email?.apply {
                it[emailKey] = this
            }
        }
    }

    override suspend fun getTokens(): UserAndToken? {
        val prefs = store.data.first()
        return prefs[accessTokenKey]?.let {
            val refreshToken = prefs[refreshTokenKey] ?: return null
            val expir = prefs[expirKey] ?: return null
            val email = prefs[emailKey] ?: return null
            UserAndToken(it, refreshToken, expir, email)
        }
    }

    override suspend fun saveOwnDetails(
        id: ULong?,
        username: String,
        rank: String?,
    ) {
        store.edit {
            id?.apply { it[idKey] = this.toLong() }
            it[userNameKey] = username
            rank?.apply { it[rankKey] = this }
        }
    }

    override suspend fun getOwnDetails(): UserDetailsAndToken? {
        val prefs = store.data.first()
        return prefs[accessTokenKey]?.let {
            val id = prefs[idKey] ?: return null
            val username = prefs[userNameKey] ?: return null
            val rank = prefs[rankKey] ?: return null
            UserDetailsAndToken(id.toULong(), username, rank, it)
        }
    }

    override suspend fun clear() {
        try {
            store.edit(MutablePreferences::clear)
        } catch (e: IOException) {
            Logger.e(tag = "IOException while clearing") { e.message.toString() }
        }
    }
}
