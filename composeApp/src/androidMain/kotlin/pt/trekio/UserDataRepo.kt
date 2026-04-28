package pt.trekio

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import pt.trekio.misc.UserAndToken
import pt.trekio.repos.UserRepo

class UserDataRepo(
    private val store: DataStore<Preferences>,
) : UserRepo {
    private val userKey = stringPreferencesKey("userName")
    private val tokenKey = stringPreferencesKey("userToken")
    private val expirKey = longPreferencesKey("expiration")

    override suspend fun saveToken(
        token: String,
        expiration: Long,
        email: String?,
    ) {
        store.edit {
            it[tokenKey] = token
            it[expirKey] = expiration
            email?.apply {
                it[userKey] = this
            }
        }
    }

    override suspend fun getToken(): UserAndToken? {
        val prefs = store.data.first()
        return prefs[tokenKey]?.let {
            val expir = prefs[expirKey] ?: return null
            val username = prefs[userKey] ?: return null
            UserAndToken(username, it, expir)
        }
    }

    override suspend fun clear() {
        store.edit(MutablePreferences::clear)
    }
}
