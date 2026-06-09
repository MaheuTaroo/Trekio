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
import pt.trekio.repos.UserRepo

fun createDataStore(storage: Storage<Preferences>): DataStore<Preferences> = DataStoreFactory.create(storage = storage)

internal const val DATASTORE_FILENAME = "trekio.preferences_pb"

class UserDataRepo(
    private val store: DataStore<Preferences>,
) : UserRepo {
    private val accessTokenKey = stringPreferencesKey("accessToken")
    private val refreshTokenKey = stringPreferencesKey("refreshToken")
    private val emailKey = stringPreferencesKey("email")
    private val expirKey = longPreferencesKey("expiration")

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

    override suspend fun clear() {
        try {
            store.edit(MutablePreferences::clear)
        } catch (e: IOException) {
            Logger.e("IOException while clearing") { e.message.toString() }
        }
    }
}
